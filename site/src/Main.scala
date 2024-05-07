import scala.scalajs.js.annotation._
import org.scalajs.dom

val text: dom.html.TextArea =
  dom.document.getElementById("output").asInstanceOf[dom.html.TextArea]

@JSExportTopLevel("updateyaml")
def update(str: String): Unit = {
  try {
    text.classList.remove("error")
    val value = yamlesque.read(str)
    text.value = ujson.write(ytoj(value), 2)
  } catch {
    case ex: Throwable =>
      text.classList.add("error")
      text.value = ex.getMessage
  }
}

def ytoj(y: yamlesque.Value): ujson.Value = y match {
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
  case yamlesque.Null()    => ujson.Null
}

