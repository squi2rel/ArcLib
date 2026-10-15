package com.github.squi2rel.arc.script;

import arc.Core;
import arc.Events;
import arc.util.Log;
import com.github.squi2rel.arc.Trigger;
import com.github.squi2rel.arc.event.GameEvent;
import com.github.squi2rel.arc.event.ResizeEvent;
import com.github.squi2rel.arc.event.FileDroppedEvent;
import org.mozilla.javascript.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * A shared Rhino environment with Arc's standard packages and script helpers preloaded.
 * One environment owns one persistent scope and serializes access to it.
 */
public class ScriptEnvironment{
    public record CompiledScript(String source, String name, Script script){ }

    protected final ReentrantLock lock = new ReentrantLock();
    protected final ContextFactory factory;
    protected final ScriptableObject topScope;
    protected final ScriptableObject scope;
    private final Consumer<String> output;
    private final ThreadLocal<Boolean> managedCall = new ThreadLocal<>();
    private final ThreadLocal<Runnable> instructionObserver = new ThreadLocal<>();
    private volatile Consumer<Throwable> callbackErrorHandler;

    public ScriptEnvironment(){
        this(Log::info);
    }

    public ScriptEnvironment(Consumer<String> output){
        this.output = Objects.requireNonNull(output, "output");
        factory = new ContextFactory(){
            @Override protected Context makeContext(){
                Context context = super.makeContext();
                context.setOptimizationLevel(-1);
                context.setLanguageVersion(Context.VERSION_ES6);
                context.setInstructionObserverThreshold(10_000);
                context.setApplicationClassLoader(ScriptEnvironment.this.getClass().getClassLoader());
                return context;
            }

            @Override protected void observeInstructionCount(Context context, int count){
                ScriptEnvironment.this.observeInstructionCount(context, count);
                Runnable observer = instructionObserver.get();
                if(observer != null) observer.run();
            }

            @Override protected Object doTopCall(Callable callable, Context context, Scriptable localScope,
                                                  Scriptable self, Object[] arguments){
                // JavaAdapter and interface callbacks enter here directly, including UI and HTTP callbacks.
                // They must finish with their actual result, rather than fail when another thread is running JS.
                lock.lock();
                try{
                    return super.doTopCall(callable, context, localScope, self, arguments);
                }catch(RuntimeException | Error failure){
                    return ScriptEnvironment.this.handleTopCallFailure(failure);
                }finally{
                    lock.unlock();
                }
            }
        };

        topScope = factory.call(ImporterTopLevel::new);
        scope = new ScriptableObject(){
            @Override public String getClassName(){ return "ArcScriptScope"; }

            @Override public void put(String name, Scriptable start, Object value){
                if(name.equals("$$scope")){
                    if(value == this) return;
                    if(value == null || value == Undefined.instance){
                        setParentScope(topScope);
                    }else if(value instanceof Scriptable other){
                        Set<Scriptable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
                        for(Scriptable parent = other; parent != null; parent = parent.getParentScope()){
                            if(!visited.add(parent)) throw Context.reportRuntimeError("Cyclic $$scope");
                            if(parent.getParentScope() == this){
                                parent.setParentScope(topScope);
                                break;
                            }
                        }
                        setParentScope(other);
                    }else{
                        throw Context.reportRuntimeError("$$scope must be a script scope or null");
                    }
                    super.put(name, start, value);
                    return;
                }
                super.put(name, start, value);
            }
        };
        scope.setParentScope(topScope);
        scope.setPrototype(topScope);
        scope.defineProperty("$$scope", (Object)null, 0);

        withContext(context -> {
            putProperty(topScope, "Core", Core.class);
            putProperty(topScope, "Events", Events.class);
            putProperty(topScope, "Trigger", Trigger.class);
            putProperty(topScope, "GameEvent", GameEvent.class);
            putProperty(topScope, "ResizeEvent", ResizeEvent.class);
            putProperty(topScope, "FileDroppedEvent", FileDroppedEvent.class);
            putProperty(topScope, "Log", Log.class);
            ScriptableObject.putProperty(topScope, "$p", ScriptableObject.getProperty(topScope, "Packages"));
            ScriptableObject.putProperty(topScope, "modName", "arclib");
            ScriptableObject.putProperty(topScope, "scriptName", "console.js");
            ScriptableObject.putProperty(topScope, "print", new BaseFunction(topScope, ScriptableObject.getFunctionPrototype(topScope)){
                @Override public Object call(Context context, Scriptable localScope, Scriptable self, Object[] arguments){
                    StringJoiner text = new StringJoiner(" ");
                    for(Object argument : arguments) text.add(Context.toString(argument));
                    ScriptEnvironment.this.output.accept(text.toString());
                    return Undefined.instance;
                }
            });
            evaluateResource(context, "base.js");
            evaluateResource(context, "global.js");
            return null;
        });
    }

    /** Called by Rhino periodically while interpreted code is running. */
    protected void observeInstructionCount(Context context, int count){ }

    /** Allows an embedding tool to handle failures raised by JavaScript callbacks. */
    protected Object handleTopCallFailure(Throwable failure){
        Consumer<Throwable> handler = callbackErrorHandler;
        if(managedCall.get() == null && handler != null){
            handler.accept(failure);
            return null;
        }
        if(failure instanceof RuntimeException runtime) throw runtime;
        throw (Error)failure;
    }

    /** Handles failures from callbacks invoked outside withContext; null restores normal propagation. */
    public void setCallbackErrorHandler(Consumer<Throwable> handler){
        callbackErrorHandler = handler;
    }

    protected Object wrap(Context context, Object value){
        if(value instanceof Class<?> type) return new NativeJavaClass(scope, type);
        return Context.javaToJS(value, scope);
    }

    protected Object unwrap(Object value){
        while(value instanceof Wrapper wrapper) value = wrapper.unwrap();
        return value;
    }

    public ScriptableObject scope(){
        return scope;
    }

    public boolean busy(){
        return lock.isLocked() && !lock.isHeldByCurrentThread();
    }

    public void put(String name, Object value){
        Objects.requireNonNull(name, "name");
        withContext(context -> {
            ScriptableObject.putProperty(scope, name, wrap(context, value));
            return null;
        });
    }

    public Object get(String name){
        Objects.requireNonNull(name, "name");
        return withContext(context -> unwrap(ScriptableObject.getProperty(scope, name)));
    }

    public Object evaluate(String source, String name){
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(name, "name");
        return withContext(context -> unwrap(context.evaluateString(scope, source, name, 1, null)));
    }

    public CompiledScript compile(String source, String name){
        return compile(source, name, false);
    }

    public CompiledScript compileStrict(String source, String name){
        return compile(source, name, true);
    }

    private CompiledScript compile(String source, String name, boolean strict){
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(name, "name");
        return withContext(context -> new CompiledScript(source, name,
            context.compileString(strict ? "\"use strict\";\n" + source : source, name, strict ? 0 : 1, null)));
    }

    public Object execute(CompiledScript script){
        Objects.requireNonNull(script, "script");
        return withContext(context -> {
            ScriptableObject.putProperty(scope, "scriptName", script.name());
            Object result = script.script().exec(context, scope);
            ScriptableObject.putProperty(scope, "$0", result);
            return unwrap(result);
        });
    }

    /** Runs an operation under this environment's lock and Rhino context, failing immediately if busy. */
    public final <T> T withContext(ContextAction<T> action){
        if(!lock.tryLock()) throw new IllegalStateException("The shared JavaScript scope is executing another script");
        try{
            return callManaged(action);
        }finally{
            lock.unlock();
        }
    }

    /** Like withContext, but returns fallback when another thread owns the environment. */
    public final <T> T tryWithContext(ContextAction<T> action, T fallback){
        if(!lock.tryLock()) return fallback;
        try{
            return callManaged(action);
        }finally{
            lock.unlock();
        }
    }

    /** Installs an instruction observer for this operation, restoring any enclosing observer afterward. */
    public final <T> T withContext(ContextAction<T> action, Runnable observer){
        Objects.requireNonNull(observer, "observer");
        return withContext(context -> {
            Runnable previous = instructionObserver.get();
            instructionObserver.set(observer);
            try{
                return action.run(context);
            }finally{
                if(previous == null) instructionObserver.remove();
                else instructionObserver.set(previous);
            }
        });
    }

    private <T> T callManaged(ContextAction<T> action){
        Boolean previous = managedCall.get();
        managedCall.set(true);
        try{
            return factory.call(action);
        }finally{
            if(previous == null) managedCall.remove();
            else managedCall.set(previous);
        }
    }

    private void putProperty(ScriptableObject target, String name, Object value){
        ScriptableObject.putProperty(target, name, value instanceof Class<?> type ? new NativeJavaClass(target, type) : value);
    }

    private void evaluateResource(Context context, String name){
        String path = "/com/github/squi2rel/arc/script/" + name;
        try(InputStream stream = ScriptEnvironment.class.getResourceAsStream(path)){
            if(stream == null) throw new FileNotFoundException(path);
            String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            context.evaluateString(topScope, source, name, 1, null);
        }catch(IOException failure){
            throw new UncheckedIOException("Cannot load Arc script environment resource " + path, failure);
        }
    }
}
