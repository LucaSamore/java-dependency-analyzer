package pcd.ass02.reactive.implementation

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.processors.BehaviorProcessor
import pcd.ass02.reactive.DependencyAnalyserLib
import pcd.ass02.reactive.ParserLib
import pcd.ass02.reactive.model.Dependency

class DependencyAnalyserLibImpl(private val parser: ParserLib) : DependencyAnalyserLib {
  private val dependenciesProcessor = BehaviorProcessor.create<List<Dependency>>()
  override val dependencies: Flowable<List<Dependency>> = dependenciesProcessor

  private val statusProcessor = BehaviorProcessor.createDefault("Ready")
  override val status: Flowable<String> = statusProcessor

  private var analysisSubscription: Disposable? = null
  private var classCount = 0
  private var dependencyCount = 0
  private val processedClasses = mutableSetOf<String>()

  override fun analyse(projectPath: String) {
    reset()

    analysisSubscription = parser.parseProject(projectPath)
      .buffer(2)
      .doOnNext { newDependencies ->
        dependencyCount += newDependencies.size

        newDependencies.forEach { dependency ->
          if (dependency.sourceClass !in processedClasses) {
            processedClasses.add(dependency.sourceClass)
            classCount++
          }
        }
      }
      .scan(emptyList<Dependency>()) { accumulated, newDependencies ->
        accumulated + newDependencies
      }
      .subscribe({ allDependencies ->
        dependenciesProcessor.onNext(allDependencies)
        statusProcessor.onNext("Analyzing: $classCount classes, $dependencyCount dependencies")
      }, { error ->
        statusProcessor.onNext("Analysis failed: ${error.message}")
      }, {
        statusProcessor.onNext("Completed: $classCount classes, $dependencyCount dependencies")
      })
  }

  private fun reset() {
    classCount = 0
    dependencyCount = 0
    dependenciesProcessor.onNext(emptyList())
    statusProcessor.onNext("Analyzing...")
    processedClasses.clear()

    analysisSubscription?.dispose()
    analysisSubscription = null
  }
}
