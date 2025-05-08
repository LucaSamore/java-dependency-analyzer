package pcd.ass02.reactive

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.processors.BehaviorProcessor
import pcd.ass02.reactive.model.Dependency
import pcd.ass02.reactive.parser.Parser

class DependencyAnalyser {
  private val _dependencies = BehaviorProcessor.create<List<Dependency>>()
  val dependencies: Flowable<List<Dependency>> = _dependencies

  private val _status = BehaviorProcessor.createDefault("Ready")
  val status: Flowable<String> = _status

  private val parser = Parser()
  private var subscription: Disposable? = null
  private var classCount = 0
  private var dependencyCount = 0

  private val processedClasses = mutableSetOf<String>()

  fun analyse(projectPath: String) {
    reset()

    subscription = parser.analyse(projectPath)
      .buffer(2)
      .doOnNext { newDependencies ->
        dependencyCount += newDependencies.size

        newDependencies.forEach { dep ->
          if (!processedClasses.contains(dep.from)) {
            processedClasses.add(dep.from)
            classCount++
          }
        }
      }
      .scan(emptyList<Dependency>()) { accumulatedList, newDependencies ->
        accumulatedList + newDependencies
      }
      .subscribe ({ allDependencies ->
        // when the buffer is full, emit the accumulated list
        _dependencies.onNext(allDependencies)
        println(allDependencies)
        _status.onNext("Analyzing: $classCount classes, $dependencyCount dependencies")
      },
        { error ->
          _status.onNext("Analysis failed: ${error.message}")
        },
        {
          _status.onNext("Completed")
        })
  }


  private fun reset(){
    classCount = 0
    dependencyCount = 0
    _dependencies.onNext(emptyList())
    _status.onNext("Analyzing...")
    processedClasses.clear()

    subscription?.dispose()
  }

  private fun cancel() {
    subscription?.dispose()
    subscription = null
  }

  fun dispose() {
    cancel()
  }
}
