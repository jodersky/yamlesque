import scala.scalajs.js.annotation._
import org.scalajs.dom

val text: dom.html.TextArea =
  dom.document.getElementById("output").asInstanceOf[dom.html.TextArea]

@JSExportTopLevel("updateyaml")
def update(str: String): Unit = {
  try {
    text.classList.remove("error")
    val value = yamlesque.core.read(str)
    text.value = ujson.write(ytoj(value), 2)
  } catch {
    case ex: _ =>
      text.classList.add("error")
      text.value = ex.getMessage
  }
}

def ytoj(y: yamlesque.core.Value): ujson.Value = y match {
  case yamlesque.core.Obj(fields) =>
    val j = ujson.Obj()
    for ((k, v) <- fields) {
      j.obj += k -> ytoj(v)
    }
    j
  case yamlesque.core.Arr(values) =>
    val j = ujson.Arr()
    for (v <- values) {
      j.arr += ytoj(v)
    }
    j
  case yamlesque.core.Str(x)  => ujson.Str(x)
  case yamlesque.core.Null()    => ujson.Null
}

