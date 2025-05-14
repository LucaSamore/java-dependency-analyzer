package pcd.ass02.reactive.implementation

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.processors.BehaviorProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import pcd.ass02.reactive.DependencyAnalyserLib
import pcd.ass02.reactive.ParserLib
import pcd.ass02.reactive.model.Dependency
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class DependencyAnalyserLibImpl(private val parser: ParserLib) : DependencyAnalyserLib {
  private val dependenciesProcessor = BehaviorProcessor.create<List<Dependency>>()
  override val dependencies: Flowable<List<Dependency>> = dependenciesProcessor.onBackpressureBuffer()

  private val statusProcessor = BehaviorProcessor.createDefault("Ready")
  override val status: Flowable<String> = statusProcessor

  private var analysisSubscription: Disposable? = null
  private val classCount = AtomicInteger(0)
  private val dependencyCount = AtomicInteger(0)
  private val processedClasses = ConcurrentHashMap.newKeySet<String>()

  override fun analyse(projectPath: String) {
    reset()

    analysisSubscription = parser.parseProject(projectPath)
      .buffer(2)
      .onBackpressureBuffer()
      .observeOn(Schedulers.computation())
      .doOnNext { newDependencies ->
        dependencyCount.addAndGet(newDependencies.size)
        newDependencies.forEach { dependency ->
          if (dependency.sourceClass !in processedClasses) {
            processedClasses.add(dependency.sourceClass)
            classCount.incrementAndGet()
          }
        }
      }
      .scan(emptyList<Dependency>()) { accumulated, newDependencies ->
        accumulated + newDependencies
      }
      .observeOn(Schedulers.io())
      .subscribe({ allDependencies ->
        dependenciesProcessor.onNext(allDependencies)
        statusProcessor.onNext("Analyzing: $classCount classes, $dependencyCount dependencies")
      }, { error ->
        statusProcessor.onNext("Analysis failed: ${error.message}")
      }, {
        statusProcessor.onNext("Completed")
      })
  }

  private fun reset() {
    classCount.set(0)
    dependencyCount.set(0)
    dependenciesProcessor.onNext(emptyList())
    statusProcessor.onNext("Analyzing...")
    processedClasses.clear()

    analysisSubscription?.dispose()
    analysisSubscription = null
  }
}
