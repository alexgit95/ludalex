package com.alexgit95.ludalex.images;

import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;

@Component
public class CoordinationImages {
    private final ReentrantLock verrou = new ReentrantLock(true);
    private long generation;

    public ReentrantLock verrou() { return verrou; }
    public long generation() { return generation; }
    public void invalider() { generation++; }
}