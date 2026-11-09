package local.daily.habits

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import org.junit.Rule
import org.junit.Test

class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun flightButtonAddsEventsAndUndoRemovesOne() {
        compose.onNodeWithTag("flight-add").assertIsDisplayed().performClick()
        compose.onNodeWithTag("flight-add").performClick()
        compose.onNodeWithTag("flight-count").assertTextContains("2 次", substring = true)
        compose.onNodeWithTag("flight-undo").performClick()
        compose.onNodeWithTag("flight-count").assertTextContains("1 次", substring = true)
    }
    @Test fun settingsRemainReachableFromToday() {
        compose.onNodeWithContentDescription("打开设置").performClick()
        compose.onNodeWithText("数据备份").assertExists()
    }
}
