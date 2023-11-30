package studio.kdb;

public class DictTableModel extends KTableModel {
    private final K.Dict dict;

    public DictTableModel(K.Dict obj) {
        this.dict = obj;
    }

    public boolean isKey(int column) {
        K.Flip f = (K.Flip) dict.x;

        return column < f.x.getLength();
    }

    public int getColumnCount() {
        return ((K.Flip) dict.x).x.getLength() + ((K.Flip) dict.y).x.getLength();
    }

    public String getColumnName(int col) {
        K.KSymbolVector v = ((K.Flip) dict.x).x;

        if (col >= ((K.Flip) dict.x).x.getLength()) {
            col -= ((K.Flip) dict.x).x.getLength();
            v = ((K.Flip) dict.y).x;
        }
        return v.at(col).toString(false);
    }

    public K.KBaseVector getColumn(int col) {
        K.Flip f = (K.Flip) dict.x;

        if (col >= f.x.getLength()) {
            col -= f.x.getLength();
            f = (K.Flip) dict.y;
        }

        return (K.KBaseVector) f.y.at(col);
    }
}
