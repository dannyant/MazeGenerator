package com.antonetti.mazegenerator;

import org.junit.Test;

public class MazeTest {
    @Test
    public void test() {
        Maze maze = new Maze(10, 10);
        maze.generate();
        System.out.println(maze);
    }
}
