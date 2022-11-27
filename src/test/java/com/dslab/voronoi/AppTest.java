package com.dslab.voronoi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {

  @Test
  public void shouldAnswerWithTrue() {
    assertTrue(true);
  }

  @Test
  public void linesShouldIntersect() {
    Line a = new Line(0, 0, 100, 100);
    Line b = new Line(100, 0, 0, 100);
    assertNotNull(a.intersects(b));
  }
}
