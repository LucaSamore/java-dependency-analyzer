package pcd.ass02.reactive

import io.reactivex.rxjava3.core.Flowable
import pcd.ass02.reactive.model.Dependency

interface ParserLib {
  fun parseProject(projectPath: String): Flowable<Dependency>
}
