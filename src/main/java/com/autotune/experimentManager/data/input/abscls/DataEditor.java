package com.autotune.experimentManager.data.input.abscls;

public abstract class DataEditor<T> {
    private boolean isEditing;
    private boolean isDone;

    public DataEditor() {
        this.isEditing = false;
        this.isDone = true;
    }

    public DataEditor(boolean isEditing, boolean isDone) {
        this.isDone = isDone;
        this.isEditing = isEditing;
    }

    public boolean isEditing() {
        return isEditing;
    }

    public void setEditing(boolean editing) {
        isEditing = editing;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    abstract public T edit();
    abstract public T done();
}
