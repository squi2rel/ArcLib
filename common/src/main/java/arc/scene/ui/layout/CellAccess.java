package arc.scene.ui.layout;

import arc.scene.Element;

public final class CellAccess{
    private CellAccess(){
    }

    public static Cell<?> cell(Element element){
        return element != null && element.parent instanceof Table table ? table.getCell(element) : null;
    }

    public static int column(Cell<?> cell){
        return cell.column;
    }

    public static int row(Cell<?> cell){
        return cell.row;
    }

    public static int align(Cell<?> cell){
        return cell.align;
    }

    public static void align(Cell<?> cell, int value){
        cell.align = value;
    }

    public static float padTop(Cell<?> cell){
        return cell.padTop;
    }

    public static void padTop(Cell<?> cell, float value){
        cell.padTop = value;
    }

    public static float padLeft(Cell<?> cell){
        return cell.padLeft;
    }

    public static void padLeft(Cell<?> cell, float value){
        cell.padLeft = value;
    }

    public static float padBottom(Cell<?> cell){
        return cell.padBottom;
    }

    public static void padBottom(Cell<?> cell, float value){
        cell.padBottom = value;
    }

    public static float padRight(Cell<?> cell){
        return cell.padRight;
    }

    public static void padRight(Cell<?> cell, float value){
        cell.padRight = value;
    }

    public static float computedPadTop(Cell<?> cell){
        return cell.computedPadTop;
    }

    public static float computedPadLeft(Cell<?> cell){
        return cell.computedPadLeft;
    }

    public static float computedPadBottom(Cell<?> cell){
        return cell.computedPadBottom;
    }

    public static float computedPadRight(Cell<?> cell){
        return cell.computedPadRight;
    }

    public static float minWidth(Cell<?> cell){
        return cell.minWidth;
    }

    public static void minWidth(Cell<?> cell, float value){
        cell.minWidth = value;
    }

    public static float minHeight(Cell<?> cell){
        return cell.minHeight;
    }

    public static void minHeight(Cell<?> cell, float value){
        cell.minHeight = value;
    }

    public static float maxWidth(Cell<?> cell){
        return cell.maxWidth;
    }

    public static void maxWidth(Cell<?> cell, float value){
        cell.maxWidth = value;
    }

    public static float maxHeight(Cell<?> cell){
        return cell.maxHeight;
    }

    public static void maxHeight(Cell<?> cell, float value){
        cell.maxHeight = value;
    }

    public static float fillX(Cell<?> cell){
        return cell.fillX;
    }

    public static void fillX(Cell<?> cell, float value){
        cell.fillX = value;
    }

    public static float fillY(Cell<?> cell){
        return cell.fillY;
    }

    public static void fillY(Cell<?> cell, float value){
        cell.fillY = value;
    }

    public static int expandX(Cell<?> cell){
        return cell.expandX;
    }

    public static void expandX(Cell<?> cell, int value){
        cell.expandX = value;
    }

    public static int expandY(Cell<?> cell){
        return cell.expandY;
    }

    public static void expandY(Cell<?> cell, int value){
        cell.expandY = value;
    }

    public static int colspan(Cell<?> cell){
        return cell.colspan;
    }

    public static void colspan(Cell<?> cell, int value){
        cell.colspan = value;
    }

    public static boolean uniformX(Cell<?> cell){
        return cell.uniformX;
    }

    public static void uniformX(Cell<?> cell, boolean value){
        cell.uniformX = value;
    }

    public static boolean uniformY(Cell<?> cell){
        return cell.uniformY;
    }

    public static void uniformY(Cell<?> cell, boolean value){
        cell.uniformY = value;
    }

    public static boolean endRow(Cell<?> cell){
        return cell.endRow;
    }

    public static void endRow(Cell<?> cell, boolean value){
        cell.endRow = value;
    }

    public static float elementX(Cell<?> cell){
        return cell.elementX;
    }

    public static float elementY(Cell<?> cell){
        return cell.elementY;
    }

    public static float elementWidth(Cell<?> cell){
        return cell.elementWidth;
    }

    public static float elementHeight(Cell<?> cell){
        return cell.elementHeight;
    }

    public static void flush(Cell<?> cell){
        if(cell == null || cell.getTable() == null) return;
        cell.getTable().invalidateHierarchy();
        cell.getTable().layout();
    }
}
