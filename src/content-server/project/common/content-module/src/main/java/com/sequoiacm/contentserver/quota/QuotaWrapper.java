package com.sequoiacm.contentserver.quota;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.util.concurrent.atomic.AtomicLong;

public class QuotaWrapper {

    private AtomicLong objects = new AtomicLong();
    private AtomicLong size = new AtomicLong();

    public QuotaWrapper() {
    }

    public QuotaWrapper(long objects, long size) {
        this.objects.set(objects);
        this.size.set(size);
    }

    public long getObjects() {
        return objects.get();
    }

    public void addObjects(long usedObjects) {
        this.objects.addAndGet(usedObjects);
    }

    public long getSize() {
        return size.get();
    }

    public void addSize(long usedSize) {
        this.size.addAndGet(usedSize);
    }

    public void setObjects(long objects) {
        this.objects.set(objects);
    }

    public void setSize(long size) {
        this.size.set(size);
    }

    @Override
    public String toString() {
        return "QuotaWrapper{" + "objects=" + objects + ", size=" + size + '}';
    }

    public BSONObject toBSONObject() {
        BSONObject bsonObject = new BasicBSONObject();
        bsonObject.put("objects", objects.get());
        bsonObject.put("size", size.get());
        return bsonObject;
    }
}
