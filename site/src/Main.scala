import scala.scalajs.js.annotation._
import org.scalajs.dom

@JSExportTopLevel("yaml")
object Main {

  val text: dom.html.TextArea =
    dom.document.getElementById("output").asInstanceOf[dom.html.TextArea]

  @JSExport
  def update(str: String): Unit = yamlesque.tryReadAll(str) match {
    case Left(err) =>
      text.classList.add("error")
      text.value = err
    case Right(yamls) =>
      text.classList.remove("error")
      val jsons = yamls.map(ytoj).map(j => ujson.write(j, 2))
      text.value = jsons.mkString("\n---\n")
  }

  def ytoj(y: yamlesque.Node): ujson.Value = y match {
    case yamlesque.Obj(fields) =>
      val j = ujson.Obj()
      for ((k, v) <- fields) {
        j.obj += k -> ytoj(v)
      }
      j
    case yamlesque.Arr(values) =>
      val j = ujson.Arr()
      for (v <- values) {
        j.arr += ytoj(v)
      }
      j
    case yamlesque.Str(x)  => ujson.Str(x)
    case yamlesque.Num(x)  => ujson.Num(x)
    case yamlesque.Bool(x) => ujson.Bool(x)
    case yamlesque.Null    => ujson.Null
  }

}
