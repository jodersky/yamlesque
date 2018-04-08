package yamlesque

trait YamlReader[A] {
  def read(yaml: YamlValue): A
}
trait YamlWriter[A] {
  def write(a: A): YamlValue
}
