package arc.util;

import arc.struct.*;

import java.nio.*;

/**
 * Class with static helper methods to increase the speed of array/direct buffer and direct buffer/direct buffer transfers
 * @author mzechner, xoppa
 */
public final class Buffers{
    static final Seq<ByteBuffer> unsafeBuffers = new Seq<>();
    static int allocatedUnsafe = 0;

    /**
     * Copies numFloats floats from src starting at offset to dst. Dst is assumed to be a direct {@link Buffer}. The method will
     * crash if that is not the case. The position and limit of the buffer are ignored, the copy is placed at position 0 in the
     * buffer. After the copying process the position of the buffer is set to 0 and its limit is set to numFloats * 4 if it is a
     * ByteBuffer and numFloats if it is a FloatBuffer. In case the Buffer is neither a ByteBuffer nor a FloatBuffer the limit is
     * not set. This is an expert method, use at your own risk.
     * @param src the source array
     * @param dst the destination buffer, has to be a direct Buffer
     * @param numFloats the number of floats to copy
     * @param offset the offset in src to start copying from
     */
    public static void copy(float[] src, Buffer dst, int numFloats, int offset){
        if(dst instanceof ByteBuffer)
            dst.limit(numFloats << 2);
        else if(dst instanceof FloatBuffer) dst.limit(numFloats);

        if(dst instanceof ByteBuffer){
            ByteBuffer bytes = ((ByteBuffer)dst).duplicate().order(ByteOrder.nativeOrder());
            bytes.position(0);
            bytes.asFloatBuffer().put(src, offset, numFloats);
        }else if(dst instanceof FloatBuffer){
            FloatBuffer floats = (FloatBuffer)dst;
            floats.position(0);
            floats.put(src, offset, numFloats);
        }else{
            throw new ArcRuntimeException("Can't copy floats to a " + dst.getClass().getName() + " instance");
        }
        dst.position(0);
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer}
     * instance's {@link Buffer#position()} is used to define the offset into the Buffer itself. The position will stay the same,
     * the limit will be set to position + numElements. <b>The Buffer must be a direct Buffer with native byte order. No error
     * checking is performed</b>.
     * @param src the source array.
     * @param srcOffset the offset into the source array.
     * @param dst the destination Buffer, its position is used as an offset.
     * @param numElements the number of elements to copy.
     */
    public static void copy(byte[] src, int srcOffset, Buffer dst, int numElements){
        dst.limit(dst.position() + bytesToElements(dst, numElements));
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements);
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer}
     * instance's {@link Buffer#position()} is used to define the offset into the Buffer itself. The position will stay the same,
     * the limit will be set to position + numElements. <b>The Buffer must be a direct Buffer with native byte order. No error
     * checking is performed</b>.
     * @param src the source array.
     * @param srcOffset the offset into the source array.
     * @param dst the destination Buffer, its position is used as an offset.
     * @param numElements the number of elements to copy.
     */
    public static void copy(short[] src, int srcOffset, Buffer dst, int numElements){
        dst.limit(dst.position() + bytesToElements(dst, numElements << 1));
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 1);
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer}
     * instance's {@link Buffer#position()} is used to define the offset into the Buffer itself. The position and limit will stay
     * the same. <b>The Buffer must be a direct Buffer with native byte order. No error checking is performed</b>.
     * @param src the source array.
     * @param srcOffset the offset into the source array.
     * @param numElements the number of elements to copy.
     * @param dst the destination Buffer, its position is used as an offset.
     */
    public static void copy(float[] src, int srcOffset, int numElements, Buffer dst){
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2);
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer}
     * instance's {@link Buffer#position()} is used to define the offset into the Buffer itself. The position will stay the same,
     * the limit will be set to position + numElements. <b>The Buffer must be a direct Buffer with native byte order. No error
     * checking is performed</b>.
     * @param src the source array.
     * @param srcOffset the offset into the source array.
     * @param dst the destination Buffer, its position is used as an offset.
     * @param numElements the number of elements to copy.
     */
    public static void copy(int[] src, int srcOffset, Buffer dst, int numElements){
        dst.limit(dst.position() + bytesToElements(dst, numElements << 2));
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2);
    }

    /**
     * Copies the contents of src to dst, starting from src[srcOffset], copying numElements elements. The {@link Buffer}
     * instance's {@link Buffer#position()} is used to define the offset into the Buffer itself. The position will stay the same,
     * the limit will be set to position + numElements. <b>The Buffer must be a direct Buffer with native byte order. No error
     * checking is performed</b>.
     * @param src the source array.
     * @param srcOffset the offset into the source array.
     * @param dst the destination Buffer, its position is used as an offset.
     * @param numElements the number of elements to copy.
     */
    public static void copy(float[] src, int srcOffset, Buffer dst, int numElements){
        dst.limit(dst.position() + bytesToElements(dst, numElements << 2));
        copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2);
    }

    /**
     * Copies the contents of src to dst, starting from the current position of src, copying numElements elements (using the data
     * type of src, no matter the datatype of dst). The dst {@link Buffer#position()} is used as the writing offset. The position
     * of both Buffers will stay the same. The limit of the src Buffer will stay the same. The limit of the dst Buffer will be set
     * to dst.position() + numElements, where numElements are translated to the number of elements appropriate for the dst Buffer
     * data type. <b>The Buffers must be direct Buffers with native byte order. No error checking is performed</b>.
     * @param src the source Buffer.
     * @param dst the destination Buffer.
     * @param numElements the number of elements to copy.
     */
    public static void copy(Buffer src, Buffer dst, int numElements){
        int numBytes = elementsToBytes(src, numElements);
        dst.limit(dst.position() + bytesToElements(dst, numBytes));
        copyJni(src, positionInBytes(src), dst, positionInBytes(dst), numBytes);
    }

    private static int positionInBytes(Buffer dst){
        return dst.position() << elementShift(dst);
    }

    private static int bytesToElements(Buffer dst, int bytes){
        return bytes >>> elementShift(dst);
    }

    private static int elementsToBytes(Buffer dst, int elements){
        return elements << elementShift(dst);
    }

    private static int elementShift(Buffer dst){
        if(dst instanceof ByteBuffer)
            return 0;
        else if(dst instanceof ShortBuffer || dst instanceof CharBuffer)
            return 1;
        else if(dst instanceof IntBuffer)
            return 2;
        else if(dst instanceof LongBuffer)
            return 3;
        else if(dst instanceof FloatBuffer)
            return 2;
        else if(dst instanceof DoubleBuffer)
            return 3;
        else
            throw new ArcRuntimeException("Can't copy to a " + dst.getClass().getName() + " instance");
    }

    public static FloatBuffer newFloatBuffer(int numFloats){
        ByteBuffer buffer = ByteBuffer.allocateDirect(numFloats * 4);
        buffer.order(ByteOrder.nativeOrder());
        return buffer.asFloatBuffer();
    }

    public static ShortBuffer newShortBuffer(int numShorts){
        ByteBuffer buffer = ByteBuffer.allocateDirect(numShorts * 2);
        buffer.order(ByteOrder.nativeOrder());
        return buffer.asShortBuffer();
    }

    public static ByteBuffer newByteBuffer(int numBytes){
        ByteBuffer buffer = ByteBuffer.allocateDirect(numBytes);
        buffer.order(ByteOrder.nativeOrder());
        return buffer;
    }

    public static IntBuffer newIntBuffer(int numInts){
        ByteBuffer buffer = ByteBuffer.allocateDirect(numInts * 4);
        buffer.order(ByteOrder.nativeOrder());
        return buffer.asIntBuffer();
    }

    public static void disposeUnsafeByteBuffer(ByteBuffer buffer){
        int size = buffer.capacity();
        synchronized(unsafeBuffers){
            if(!unsafeBuffers.remove(buffer, true))
                throw new IllegalArgumentException("buffer not allocated with newUnsafeByteBuffer or already disposed");
        }
        allocatedUnsafe -= size;
    }

    public static boolean isUnsafeByteBuffer(ByteBuffer buffer){
        synchronized(unsafeBuffers){
            return unsafeBuffers.contains(buffer, true);
        }
    }

    /**
     * Allocates a new direct ByteBuffer from native heap memory using the native byte order. Needs to be disposed with
     * {@link #disposeUnsafeByteBuffer(ByteBuffer)}.
     */
    public static ByteBuffer newUnsafeByteBuffer(int numBytes){
        ByteBuffer buffer = ByteBuffer.allocateDirect(numBytes);
        buffer.order(ByteOrder.nativeOrder());
        allocatedUnsafe += numBytes;
        synchronized(unsafeBuffers){
            unsafeBuffers.add(buffer);
        }
        return buffer;
    }

    /**
     * Returns the address of the Buffer, it assumes it is an unsafe buffer.
     * @param buffer The Buffer to ask the address for.
     * @return the address of the Buffer.
     */
    public static long getUnsafeBufferAddress(Buffer buffer){
        return 0L;
    }

    /**
     * Registers the given ByteBuffer as an unsafe ByteBuffer. The ByteBuffer must have been allocated in native code, pointing to
     * a memory region allocated via malloc. Needs to be disposed with {@link #disposeUnsafeByteBuffer(ByteBuffer)}.
     * @param buffer the {@link ByteBuffer} to register
     * @return the ByteBuffer passed to the method
     */
    public static ByteBuffer newUnsafeByteBuffer(ByteBuffer buffer){
        allocatedUnsafe += buffer.capacity();
        synchronized(unsafeBuffers){
            unsafeBuffers.add(buffer);
        }
        return buffer;
    }

    /** @return the number of bytes allocated with {@link #newUnsafeByteBuffer(int)} */
    public static int getAllocatedBytesUnsafe(){
        return allocatedUnsafe;
    }

    /** Writes the specified number of zeros to the buffer. */
    private static void clear(ByteBuffer buffer, int numBytes){
        ByteBuffer dup = buffer.duplicate();
        dup.position(0);
        dup.limit(Math.min(numBytes, dup.capacity()));
        while(dup.hasRemaining()){
            dup.put((byte)0);
        }
    }

    private static void copyJni(byte[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes){
        ByteBuffer bytes = byteView(dst);
        bytes.position(dstOffset);
        bytes.put(src, srcOffset, numBytes);
    }

    private static void copyJni(short[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes){
        ByteBuffer bytes = byteView(dst);
        bytes.position(dstOffset);
        bytes.asShortBuffer().put(src, srcOffset, numBytes >> 1);
    }

    private static void copyJni(int[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes){
        ByteBuffer bytes = byteView(dst);
        bytes.position(dstOffset);
        bytes.asIntBuffer().put(src, srcOffset, numBytes >> 2);
    }

    private static void copyJni(float[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes){
        ByteBuffer bytes = byteView(dst);
        bytes.position(dstOffset);
        bytes.asFloatBuffer().put(src, srcOffset, numBytes >> 2);
    }

    public static void copyJni(Buffer src, int srcOffset, Buffer dst, int dstOffset, int numBytes){
        ByteBuffer srcBytes = byteView(src);
        ByteBuffer dstBytes = byteView(dst);
        Java16Buffers.copy(srcBytes, srcOffset, dstBytes, dstOffset, numBytes);
    }

    private static ByteBuffer byteView(Buffer buffer){
        if(buffer instanceof ByteBuffer){
            return ((ByteBuffer)buffer).duplicate().order(ByteOrder.nativeOrder());
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(buffer.capacity() << elementShift(buffer)).order(ByteOrder.nativeOrder());
        if(buffer instanceof ShortBuffer){
            ShortBuffer src = ((ShortBuffer)buffer).duplicate();
            ShortBuffer dst = bytes.asShortBuffer();
            src.position(0);
            dst.put(src);
        }else if(buffer instanceof IntBuffer){
            IntBuffer src = ((IntBuffer)buffer).duplicate();
            IntBuffer dst = bytes.asIntBuffer();
            src.position(0);
            dst.put(src);
        }else if(buffer instanceof FloatBuffer){
            FloatBuffer src = ((FloatBuffer)buffer).duplicate();
            FloatBuffer dst = bytes.asFloatBuffer();
            src.position(0);
            dst.put(src);
        }else{
            throw new ArcRuntimeException("Can't view a " + buffer.getClass().getName() + " instance as bytes");
        }
        return bytes;
    }
}
