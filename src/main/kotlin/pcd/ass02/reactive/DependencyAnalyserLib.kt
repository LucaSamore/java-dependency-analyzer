package pcd.ass02.reactive

import io.reactivex.rxjava3.core.Flowable
import pcd.ass02.reactive.model.Dependency

interface DependencyAnalyserLib {
  val dependencies: Flowable<List<Dependency>>
  val status: Flowable<String>
  fun analyse(projectPath: String)
}
