package com.example.neatgrid

import com.example.neatgrid.data.GameMetadata
import com.example.neatgrid.data.LaunchBoxService
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LaunchBoxServiceTest {

    @Test
    fun testParseSearchResults_isCorrect() {
        val html = """
            <html>
            <body>
                <div class="games-grid-card">
                    <a class="list-item" href="/games/details/2224-sonic-advance">
                        <div class="cardImgPart">
                            <img src="https://images.launchbox-app.com/r2_1294b80c.jpg" />
                        </div>
                        <div class="cardContent">
                            <div class="cardTitle">
                                <h3>Sonic Advance</h3>
                                <p>Nintendo Game Boy Advance</p>
                            </div>
                            <div class="releaseDate">
                                <h5>December 20, 2001</h5>
                            </div>
                            <div class="rateit" data-rateit-value="3.946"></div>
                        </div>
                    </a>
                </div>
            </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(html, "https://gamesdb.launchbox-app.com")
        val service = LaunchBoxService()
        val results = service.parseSearchResults(doc)

        assertEquals(1, results.size)
        val game = results[0]
        assertEquals("Sonic Advance", game.title)
        assertEquals(null, game.summary)
        assertEquals("2224-sonic-advance", game.sourceId)
        assertEquals("https://images.launchbox-app.com/r2_1294b80c.jpg", game.coverUrl)
        assertEquals(1, game.platforms.size)
        assertEquals("Nintendo Game Boy Advance", game.platforms[0])
        assertEquals("December 20, 2001", game.releaseDate)
        assertEquals(3.946 * 20.0, game.rating!!, 0.01)
    }

    @Test
    fun testParseGameDetails_isCorrect() {
        val html = """
            <html>
            <head>
                <title>Sonic Advance - LaunchBox Games Database</title>
            </head>
            <body>
                <main>
                    <img class="opacity-40" src="https://images.launchbox-app.com/hero.jpg" />
                    <h1>Sonic Advance</h1>
                </main>
                <div id="overview">
                    <p class="text-dark-100">Sonic the Hedgehog has arrived for his first ever adventure.</p>
                    <p class="text-dark-100">Choose from Sonic, Knuckles, Tails, or Amy.</p>
                </div>
                <dl>
                    <dt>Genre</dt>
                    <dd>
                        <a href="/genres/1-Action">Action</a>, 
                        <a href="/genres/10-Platform">Platform</a>
                    </dd>
                    <dt>Platform</dt>
                    <dd>
                        <a href="/platforms/29-GBA">Nintendo Game Boy Advance</a>
                    </dd>
                </dl>
                <h3>Box - Front</h3>
                <div>
                    <img src="https://images.launchbox-app.com/box-front.jpg" />
                </div>
                <h3>Screenshot - Gameplay</h3>
                <div>
                    <img src="https://images.launchbox-app.com/gameplay1.jpg" />
                    <img src="https://images.launchbox-app.com/gameplay2.jpg" />
                </div>
            </body>
            </html>
        """.trimIndent()

        val doc = Jsoup.parse(html, "https://gamesdb.launchbox-app.com")
        val service = LaunchBoxService()
        val basicMetadata = GameMetadata(
            title = "Sonic Advance",
            summary = "launchbox:2224-sonic-advance",
            rating = 78.9,
            releaseDate = "December 20, 2001",
            genres = emptyList(),
            platforms = listOf("Nintendo Game Boy Advance"),
            coverUrl = "https://images.launchbox-app.com/cover-preview.jpg",
            screenshotUrls = emptyList()
        )

        val gameDetails = service.parseGameDetails(doc, basicMetadata)

        assertNotNull(gameDetails)
        assertEquals("Sonic Advance", gameDetails.title)
        assertEquals(
            "Sonic the Hedgehog has arrived for his first ever adventure.\n\nChoose from Sonic, Knuckles, Tails, or Amy.",
            gameDetails.summary
        )
        assertEquals("https://images.launchbox-app.com/box-front.jpg", gameDetails.coverUrl)
        assertEquals(2, gameDetails.genres.size)
        assertEquals("Action", gameDetails.genres[0])
        assertEquals("Platform", gameDetails.genres[1])
        assertEquals(3, gameDetails.screenshotUrls.size)
        assertEquals("https://images.launchbox-app.com/hero.jpg", gameDetails.screenshotUrls[0])
        assertEquals("https://images.launchbox-app.com/gameplay1.jpg", gameDetails.screenshotUrls[1])
        assertEquals("https://images.launchbox-app.com/gameplay2.jpg", gameDetails.screenshotUrls[2])
    }
}
