import org.junit.runner.RunWith
import org.scalatest.{ WordSpecLike, Matchers }
import org.scalatest.prop.PropertyChecks
import org.scalatest.junit.JUnitRunner
import org.scalacheck._
import play.api.libs.json._
import play.api.libs.json.Json._

class ModelSpec extends WordSpecLike with Matchers with PropertyChecks {

  import models._
  implicit override val generatorDrivenConfig = PropertyCheckConfig(minSize = 1, maxSize = 100, minSuccessful = 100, workers = 5)

  val LatGenerator = Gen.chooseNum(-90d, 90d)
  val LngGenerator = Gen.chooseNum(-180d, 180d) suchThat (_ < 180)
  val TimeGenerator = Gen.chooseNum(0L, Long.MaxValue)
  val LatLngGenerator = for { lat <- LatGenerator; lng <- LngGenerator } yield LatLng(lat, lng)
  val PositionGenerator = for { l <- LatLngGenerator; t <- TimeGenerator } yield Position(l, t)
  val NameGenerator = Gen.alphaStr
  val DoubleGenerator = Gen.chooseNum(0d, 1000d) suchThat (_ > 0)

  "A Model" must {

    "provide class LatLng modelling GPS coordinates" which {
      "should be serializable to/from json array" in {
        forAll(LatGenerator, LngGenerator) { (a: Double, b: Double) =>
          val c = LatLng(a, b)
          c.latitude should be(a)
          c.longitude should be(b)
          toJson(c) should be(JsArray(Seq(JsNumber(a), JsNumber(b))))
          parse(s"[$a,$b]").as[LatLng] should be(c)
        }
      }
    }

    "provide class Position modelling GPS position at time" which {
      "should be serializable to/from json object" in {
        forAll(LatLngGenerator, TimeGenerator) { (c: LatLng, t: Long) =>
          val p = Position(c, t)
          p.time should be(t)
          p.location should be(c)
          toJson(p) should be(JsObject(Map("location" -> toJson(c), "time" -> JsNumber(t))))
          parse(s"""{"time": $t, "location": ${stringify(toJson(c))}}""").as[Position] should be(p)
        }
      }
    }

    "provide class Vessel modelling ship at the sea" which {
      "should be serializable to/from json object" in {
        forAll(NameGenerator, DoubleGenerator, DoubleGenerator, DoubleGenerator, PositionGenerator) {
          (n: String, w: Double, l: Double, d: Double, p: Position) =>
            val v = Vessel(n, w, l, d, p)
            v.name should be(n)
            v.width should be(w)
            v.length should be(l)
            v.draft should be(d)
            v.lastSeenPosition should be(p)
            toJson(v) should be(Json.obj(
              "name" -> JsString(n), "width" -> JsNumber(w), "length" -> JsNumber(l), "draft" -> JsNumber(d),
              "lastSeenPosition" -> toJson(p)
            ))
            parse(s"""{"width":$w, "length":$l, "name":"$n", "lastSeenPosition":${stringify(toJson(p))}, "draft":$d}""").as[Vessel] should be(v)
        }
      }
    }
  }
}