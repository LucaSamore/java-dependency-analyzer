package pcd.ass02;

import kotlin.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class Sample implements Runnable, Comparable<Unit> {

  private String example;

  @Override
  public void run() {
    System.out.println("example");
  }

  private Optional<String> getExample() {
    return Optional.ofNullable(this.example);
  }

  @Override
  public int compareTo(@NotNull Unit o) {
    return 0;
  }
}
