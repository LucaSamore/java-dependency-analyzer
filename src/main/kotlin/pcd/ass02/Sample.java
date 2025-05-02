package pcd.ass02;

import java.util.Optional;

public final class Sample implements Runnable {

  private String example;

  @Override
  public void run() {
    System.out.println("example");
  }

  private Optional<String> getExample() {
    return Optional.ofNullable(this.example);
  }
}
