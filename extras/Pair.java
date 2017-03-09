package com.dominicapps.dictionary.tablelab.extras;

import com.dominicapps.dictionary.tablelab.TableLab;

import org.joda.time.DateTime;

/**
 * Reusable class for Many to Many relationships. Speeds up classic
 * id, left_id, right_id.
 */

public class Pair {

    private int id, left, right;
    private DateTime created, updated;

    public Pair() {
    }

    public Pair(int id, int left, int right, DateTime created, DateTime updated) {
        this.id = id;
        this.left = left;
        this.right = right;
        this.created = created;
        this.updated = updated;
    }

    public Pair(int left, int right, DateTime created, DateTime updated) {
        this.left = left;
        this.right = right;
        this.created = created;
        this.updated = updated;
    }

    public Pair(int left, int right, DateTime updated) {
        this.left = left;
        this.right = right;
        this.updated = updated;
    }

    public Pair(int left, int right) {
        this.left = left;
        this.right = right;
        created = TableLab.now();
        updated = TableLab.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLeft() {
        return left;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public int getRight() {
        return right;
    }

    public void setRight(int right) {
        this.right = right;
    }

    public DateTime getCreated() {
        return created;
    }

    public void setCreated(DateTime created) {
        this.created = created;
    }

    public DateTime getUpdated() {
        return updated;
    }

    public void setUpdated(DateTime updated) {
        this.updated = updated;
    }
}
