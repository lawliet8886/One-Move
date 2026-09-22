package com.example
import com.example.onemove.model.*
object InputContractCases {
    fun run(): Int {
        var checks=0
        fun verify(value: Boolean, message: String) { checks++; check(value) { message } }
        // Overlapping targets must select the nearest handle independent of list order.
        val pA = Pin(PinId.PIN_A, "A", start = Vector2D(100f,100f), end = Vector2D(300f,100f), handlePosition = Vector2D(100f,100f))
        val pB = Pin(PinId.PIN_B, "B", start = Vector2D(120f,100f), end = Vector2D(320f,100f), handlePosition = Vector2D(120f,100f))
        verify(PinHitTester.find(listOf(pA,pB),119f,100f)==PinId.PIN_B,"First-list pin stole nearer handle")
        verify(PinHitTester.find(listOf(pB,pA),119f,100f)==PinId.PIN_B,"Reordering changes selection")
        verify(PinHitTester.find(listOf(pA.copy(isRemoved=true),pB),100f,100f)==PinId.PIN_B,"Removed pin still interactive")
        verify(PinHitTester.find(listOf(pA,pB),Float.NaN,100f)==null,"Non-finite touch accepted")
        verify(PinHitTester.find(listOf(pA,pB),600f,600f)==null,"Distant empty-board tap consumed a move")
        for (widthDp in listOf(280f,320f,360f,412f,600f)) {
            val factor=widthDp/LevelDefinition.WORLD_WIDTH
            val radius=maxOf(65f,24f/factor)
            verify(radius*2f*factor>=48f-0.001f,"Touch diameter below 48dp")
            verify(PinHitTester.find(listOf(pA),100f,100f+radius-0.001f,radius)==PinId.PIN_A,"Minimum physical touch target fails")
        }
        return checks
    }
}
