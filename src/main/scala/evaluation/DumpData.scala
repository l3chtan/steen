package evaluation

import java.io.File
import java.util.concurrent.TimeUnit

import com.quantifind.charts.highcharts._
import com.quantifind.charts.highcharts.Highchart._

/**
  * Created by FM on 08.08.16.
  */
case class DumpData(configCaption: String, instanceSizeCaption: String) {

  def printResults(filePath: String)(results: Seq[AlgorithmResult]) {
    printToFile(new File(filePath)) { p =>
      val captions = Seq(
        configCaption,
        instanceSizeCaption,
        "Append-Min [ms]",
        "Append-Max [ms]",
        "Append-Avg [ms]",
        "Append-Median [ms]",
        "Evaluate-Min [ms]",
        "Evaluate-Max [ms]",
        "Evaluate-Avg [ms]",
        "Evaluate-Median [ms]"
      )

      p.println(captions.mkString(";"))

      val resultStrings = results.map(a => a.runs.map(r => Seq(a.caption, r.instanceCaption) ++ configResultFormatted(r)))

      resultStrings foreach (r => p.println(r.mkString(";")))
    }
  }

  def configResult(config: ConfigurationResult) = {
    config.appendResult.asResult() ++ config.evaluateResult.asResult()
  }

  def configResultFormatted(config: ConfigurationResult) = {
    configResult(config) map (_.formatted("%f"))
  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try {
      op(p)
    } finally {
      p.close()
    }
  }


  def plot(results: Seq[AlgorithmResult]): Unit = {
    val series = results map dataSeries

    val xAxis = Axis(categories = Some(results.head.runs.map(_.instanceCaption).toArray))
    val c = Highchart(series,
      chart = Chart(zoomType = Zoom.xy),
      xAxis = Some(Array(xAxis)),
      yAxis = Some(Array(Axis(title = Some(AxisTitle("Median [ms]")))))
    )
    com.quantifind.charts.Highcharts.plot(c)
  }

  def dataSeries(result: AlgorithmResult) = {
    val data = result.runs.zipWithIndex.map {
      case (r, i) => Data(i, r.appendResult.median.toUnit(TimeUnit.MILLISECONDS), name = r.instanceCaption)
    }
    Series(data, name = Some(result.caption))
  }
}
