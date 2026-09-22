import com.example.PhysicsContractCases
import com.example.InputContractCases
import java.io.File
fun main() {
    val report=PhysicsContractCases.run()
    val inputChecks=InputContractCases.run()
    val dir=File("build/headless").apply{mkdirs()}
    File(dir,"campaign.csv").writeText(report.rows.joinToString("\n"))
    File(dir,"summary.txt").writeText("PASS: ${report.simulations} simulations; ${report.checks+inputChecks} checks; longest winning route ${report.maxWinSeconds}s. Production Kotlin physics with value-only Compose adapters and no-op vibration. No APK, Android runtime, UI or video was exercised.\n")
    println(File(dir,"summary.txt").readText())
}
