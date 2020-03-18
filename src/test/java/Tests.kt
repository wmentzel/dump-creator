import com.randomlychosenbytes.openfoodfactsdumper.getFirstNumberInString
import org.junit.Test
import kotlin.test.assertEquals

class Tests {
    @Test
    fun shouldParsePortionsCorrectly() {
        assertEquals(33.3f, getFirstNumberInString("33,3g"))
        assertEquals(33.3f, getFirstNumberInString("33,3"))
        assertEquals(33.3f, getFirstNumberInString("33.3g"))
        assertEquals(33.3f, getFirstNumberInString("33.3"))
        assertEquals(33.3f, getFirstNumberInString("foo33.3bar"))
        assertEquals(33f, getFirstNumberInString("foo33foo.3bar"))
        assertEquals(null, getFirstNumberInString("foo"))
    }
}