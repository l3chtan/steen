package evaluation

import java.io.File
import java.util.concurrent.TimeUnit

import com.quantifind.charts.highcharts._
import com.quantifind.charts.highcharts.Highchart._
import com.quantifind.charts.repl.HighchartsStyles

/**
  * Created by FM on 08.08.16.
  */
case class DumpData(configCaption: String, instanceSizeCaption: String) {

  def printResults(filePath: String)(results: Seq[AlgorithmResult[TimingsConfigurationResult]]) {
    printToFile(new File(filePath)) { printer => //TODO abstraction needed for comparison (clingo pull needs evaluate; others need append)
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

      printer println (captions.mkString(";"))

      val resultStrings = results map (a => a.runs flatMap (r => Seq(a.caption, r.instanceCaption) ++ configResultFormatted(r)))

      resultStrings foreach (r => printer println (r.mkString(";")))
    }
  }

  def printSuccessResults(filePath: String)(results: Seq[AlgorithmResult[SuccessConfigurationResult]]): Unit = {
    printToFile(new File(filePath)) { printer =>
      val captions = Seq(
        configCaption,
        instanceSizeCaption,
        "Timepoint",
        "Found Model"
      )

      printer.println(captions.mkString(";"))

      val resultStrings = results.flatMap(a => a.runs flatMap (r => r.successFailures map {
        case (time, success) => Seq(a.caption, r.instanceCaption, time.toString, success.toString)
      }))

      resultStrings foreach (r => printer.println(r.mkString(";")))
    }
  }

  def configResult(config: TimingsConfigurationResult) = {
    config.appendResult.asResult() ++ config.evaluateResult.asResult()
  }

  def configResultFormatted(config: TimingsConfigurationResult) = {
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

  def plot(results: Seq[AlgorithmResult[TimingsConfigurationResult]]): Unit = {
    val series = results map dataSeriesAppendMedian //TODO generalize

    val xAxis = Axis(
      categories = Some(results.head.runs.map(_.instanceCaption).toArray),
      axisType = Some(AxisType.category)
    )
    val c = Highchart(series,
      chart = Chart(zoomType = Zoom.xy),

      xAxis = Some(Array(xAxis)),
      yAxis = Some(Array(Axis(title = Some(AxisTitle("Median [ms]")))))
    )

    plot(c)
  }

  def dataSeriesAppendMedian(result: AlgorithmResult[TimingsConfigurationResult]) = {
    val data = result.runs.zipWithIndex.map {
      case (r, i) => Data(i, r.appendResult.median.toUnit(TimeUnit.MILLISECONDS), name = r.instanceCaption)
    }
    Series(data, name = Some(result.caption), chart = SeriesType.column)
  }

  def plotFailures(results: Seq[AlgorithmResult[SuccessConfigurationResult]]): Unit = {
    val series = results flatMap dataSeriesFailures


    val xAxis = Axis(
      categories = Some(results.head.runs.map(_.instanceCaption).toArray),
      labels = AxisLabel(
        step = (results map (r => r.runs.map(rr => rr.successFailures.size) sum) sum) / 20,
        maxStaggerLines = 1
      )
    )
    val c = Highchart(series,
      chart = Chart(zoomType = Zoom.xy),
      xAxis = Some(Array(xAxis)),
      yAxis = Some(Array(Axis(title = Some(AxisTitle("Failures")))))
    )
    plot(c)
  }

  def plot(chart: Highchart): Unit = {
    new HighchartsStyles {
      override def reloadJs = ""
    } plot (chart)
  }

  def failureComputed(wasModelComputed: Boolean) = true.compareTo(wasModelComputed)

  def dataSeriesFailures(result: AlgorithmResult[SuccessConfigurationResult]) = {

    val series = result.runs.map(
      r => {
        val aggregatedFailures = r.successFailures.scanLeft((0, 0)) {
          case (agg, value) => (value._1, agg._2 + failureComputed(value._2))
        } map {
          sf => Data(sf._1, sf._2)
        }
        (r.instanceCaption, aggregatedFailures)
      })
    series map (s => Series(s._2, name = Some(result.caption + s._1), chart = SeriesType.line))
  }
}
