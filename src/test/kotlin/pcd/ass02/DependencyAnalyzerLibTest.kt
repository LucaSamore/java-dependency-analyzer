package pcd.ass02

import io.vertx.core.Vertx
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pcd.ass02.async.DependencyAnalyzerLib
import pcd.ass02.async.implementation.DependencyAnalyzerLibImpl

@RunWith(VertxUnitRunner::class)
class DependencyAnalyzerLibImplTest {

  private lateinit var vertx: Vertx
  private lateinit var analyzer: DependencyAnalyzerLib
  private val testResourcesDir = Path.of("src/test/resources/depanalyzer")

  @Before
  fun setUp(context: TestContext) {
    vertx = Vertx.vertx()
    analyzer = DependencyAnalyzerLibImpl(vertx)
    val async = context.async()
    setupTestFiles().onComplete {
      if (it.succeeded()) {
        async.complete()
      } else {
        context.fail(it.cause())
      }
    }
  }

  @After
  fun tearDown(context: TestContext) {
    val async = context.async()

    try {
      Files.walk(testResourcesDir).sorted(Comparator.reverseOrder()).forEach {
        Files.deleteIfExists(it)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    vertx.close { res ->
      if (res.succeeded()) {
        async.complete()
      } else {
        context.fail(res.cause())
      }
    }
  }

  @Test
  fun testGetClassDependencies(context: TestContext) {
    val async = context.async()

    val testFilePath = testResourcesDir.resolve("TestClass.java")

    analyzer
        .getClassDependencies(testFilePath)
        .onSuccess { report ->
          context.assertEquals("pcd.ass02.test.TestClass", report.className)
          context.assertTrue(report.usedTypes.contains("BaseClass"))
          context.assertTrue(report.usedTypes.contains("Serializable"))
          context.assertTrue(report.usedTypes.contains("List<String>"))
          context.assertTrue(report.usedTypes.contains("String"))
          context.assertTrue(report.usedTypes.contains("void"))
          async.complete()
        }
        .onFailure { context.fail(it) }
  }

  @Test
  fun testGetPackageDependencies(context: TestContext) {
    val async = context.async()

    val packagePath = testResourcesDir.resolve("testpackage")

    analyzer
        .getPackageDependencies(packagePath)
        .onSuccess { report ->
          context.assertEquals("testpackage", report.packageName)
          context.assertEquals(2, report.classReports.size)

          val class1 = report.classReports.find { it.className == "pcd.ass02.testpackage.Class1" }
          val class2 = report.classReports.find { it.className == "pcd.ass02.testpackage.Class2" }

          context.assertNotNull(class1)
          context.assertNotNull(class2)

          context.assertTrue(class1!!.usedTypes.contains("Map<String,Integer>"))
          context.assertTrue(class2!!.usedTypes.contains("Set<String>"))

          context.assertTrue(class1.usedTypes.contains("String"))
          context.assertTrue(class1.usedTypes.contains("Integer"))
          context.assertTrue(class1.usedTypes.contains("void"))

          context.assertTrue(class2.usedTypes.contains("String"))
          context.assertTrue(class2.usedTypes.contains("boolean"))

          async.complete()
        }
        .onFailure { context.fail(it) }
  }

  @Test
  fun testGetProjectDependencies(context: TestContext) {
    val async = context.async()

    val projectPath = testResourcesDir.resolve("testproject")

    analyzer
        .getProjectDependencies(projectPath)
        .onSuccess { report ->
          context.assertEquals(2, report.packageReports.size)

          val pkg1 = report.packageReports.find { it.packageName == "package1" }
          val pkg2 = report.packageReports.find { it.packageName == "package2" }

          context.assertNotNull(pkg1)
          context.assertNotNull(pkg2)

          context.assertEquals(1, pkg1!!.classReports.size)
          context.assertEquals(1, pkg2!!.classReports.size)

          val serviceClass = pkg1.classReports[0]
          val clientClass = pkg2.classReports[0]

          context.assertTrue(serviceClass.usedTypes.contains("Future<String>"))
          context.assertTrue(serviceClass.usedTypes.contains("String"))

          context.assertTrue(clientClass.usedTypes.contains("Service"))
          context.assertTrue(clientClass.usedTypes.contains("String"))
          context.assertTrue(clientClass.usedTypes.contains("void"))

          async.complete()
        }
        .onFailure { context.fail(it) }
  }

  private fun setupTestFiles() =
      vertx.executeBlocking(
          Callable {
            Files.createDirectories(testResourcesDir)

            createTestClassFile()
            createTestPackage()
            createTestProject()
          })

  private fun createTestClassFile() {
    val content =
        """
            package pcd.ass02.test;

            import java.io.Serializable;

            public class TestClass extends BaseClass implements Serializable {
                private List<String> items;

                public void doSomething(String text) {
                    items.add(text);
                }
            }
        """
            .trimIndent()

    val file = testResourcesDir.resolve("TestClass.java")
    Files.writeString(file, content)
  }

  private fun createTestPackage() {
    val packageDir = testResourcesDir.resolve("testpackage")
    Files.createDirectories(packageDir)

    val class1Content =
        """
            package pcd.ass02.testpackage;

            public class Class1 {
                private Map<String, Integer> data;

                public void processData(String key, Integer value) {
                    data.put(key, value);
                }
            }
        """
            .trimIndent()

    val class2Content =
        """
            package pcd.ass02.testpackage;

            public class Class2 {
                private Set<String> tags;

                public boolean hasTag(String tag) {
                    return tags.contains(tag);
                }
            }
        """
            .trimIndent()

    Files.writeString(packageDir.resolve("Class1.java"), class1Content)
    Files.writeString(packageDir.resolve("Class2.java"), class2Content)
  }

  private fun createTestProject() {
    val projectDir = testResourcesDir.resolve("testproject")
    Files.createDirectories(projectDir)

    val package1Dir = projectDir.resolve("package1")
    val package2Dir = projectDir.resolve("package2")

    Files.createDirectories(package1Dir)
    Files.createDirectories(package2Dir)

    val pkg1ClassContent =
        """
            package pcd.ass02.package1;

            public class Service {
                public Future<String> process(String input) {
                    return null;
                }
            }
        """
            .trimIndent()

    val pkg2ClassContent =
        """
            package pcd.ass02.package2;

            public class Client {
                private Service service;

                public void execute(String command) {
                    service.process(command);
                }
            }
        """
            .trimIndent()

    Files.writeString(package1Dir.resolve("Service.java"), pkg1ClassContent)
    Files.writeString(package2Dir.resolve("Client.java"), pkg2ClassContent)
  }
}
