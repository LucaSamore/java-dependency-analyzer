package pcd.ass02.reactive.implementation

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.processors.BehaviorProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import pcd.ass02.reactive.DependencyAnalyserLib
import pcd.ass02.reactive.ParserLib
import pcd.ass02.reactive.model.Dependency

class DependencyAnalyserLibImpl(private val parser: ParserLib) : DependencyAnalyserLib {
  private val dependenciesProcessor = BehaviorProcessor.create<List<Dependency>>()
  override val dependencies: Flowable<List<Dependency>> = dependenciesProcessor.onBackpressureBuffer()

  private val statusProcessor = BehaviorProcessor.createDefault("Ready")
  override val status: Flowable<String> = statusProcessor.onBackpressureBuffer()

  private var analysisSubscription: Disposable? = null

  override fun analyse(projectPath: String) {
    reset()

    analysisSubscription = parser.parseProject(projectPath)
      .buffer(20)
      .onBackpressureBuffer()
      .observeOn(Schedulers.computation())
      .scan(emptyList<Dependency>()) { accumulated, newDependencies ->
        accumulated + newDependencies
      }
      .map { dependencies ->
        val uniqueClasses = dependencies.flatMap {
          listOf(it.sourceClass, it.targetClass)
        }.toSet()

        Pair(dependencies, uniqueClasses)
      }
      .observeOn(Schedulers.io())
      .subscribe({ (dependencies, uniqueClasses) ->
        dependenciesProcessor.onNext(dependencies)
        statusProcessor.onNext("Analyzing: ${uniqueClasses.size} classes, ${dependencies.size} dependencies")
      }, { error ->
        statusProcessor.onNext("Analysis failed: ${error.message}")
      }, {
        statusProcessor.onNext("Analysis completed")
      })
  }
  private fun reset() {
    dependenciesProcessor.onNext(emptyList())
    statusProcessor.onNext("Analyzing...")
    analysisSubscription?.dispose()
    analysisSubscription = null
  }
}
