# Film-mood keyboard themes (research for 20 Akkhara themes)

Research date: 2026-11-20. Research only; no repository was changed.

## Summary

- **Box office in 2026 is led by a street-level superhero film.** *Spider-Man: Brand New Day* (Sony, 31 July 2026) has grossed $2.52 bn worldwide, the top 2026 release on The Numbers, and an extended cut went back into cinemas on 19 November 2026. ([The Numbers, film page](https://www.the-numbers.com/movie/Spider-Man-Brand-New-Day-(2026)); [The Numbers, 2026 worldwide chart](https://www.the-numbers.com/box-office-records/worldwide/all-movies/cumulative/released-in-2026))
- **_Avengers: Doomsday_ is not out yet.** It opens on 18 December 2026 (Disney). It has no box office yet, but tracking in mid-November put its opening at about $410M. ([The Numbers](https://www.the-numbers.com/movie/Avengers-Doomsday-(2026)); [ComicBookMovie tracking](https://comicbookmovie.com/avengers/avengers-doomsday/avengers-doomsday-early-tracking-points-to-gargantuan-410-million-opening-weekend-a230990)) Its palette comes from trailers only.
- **The rest of the picks:**
  - Comedy: the 2026 comedy hits (*The Devil Wears Prada 2*, *Scary Movie*, *Pegasus 3*, *The Sheep Detectives*, *Coyote vs. Acme*) plus the Thanksgiving release *Focker-In-Law*, which opens on 25 or 26 November 2026.
  - Animation: the biggest animated films of 2025 and 2026 that the earlier theme set did not use.
  - Superhero: the 2025 and 2026 DC and Marvel releases.
- **The palettes:** 20 palettes, 6 comedy + 7 animated + 7 superhero. Every key style is used at least twice (Rounded 2, all others 3). All 20 pass every contrast rule in `contrast_check.py`.
  - The lowest ratio under a 4.5:1 rule is 4.88 (Atomic's action label).
  - The lowest accent-against-background ratio under the 3:1 rule is 3.86 (Zenith).
- **Checks:** the research used a one-off script. ThemesTest now holds every theme to the same floors (see Part 4).

Excluded on request (earlier theme set): Godzilla Minus Zero, Ebenezer, Ramayana, Clayface, You Can See Everything, The Cat in the Hat, Wildwood, Hexed, KPop Demon Hunters, Sing, Toy Story 5, The Super Mario Galaxy Movie, Hoppers, Minions & Monsters, The Violinist, Chiikawa, Frieren, The Apothecary Diaries, Dragon Ball Super, Firefly Wedding, Dead-End Job, Lupin, A Dog's Perfect Christmas.

---

## Part 1: Sources and chosen titles

### 1a. Sources consulted

| # | Source | Type | What it was used for |
|---|---|---|---|
| S1 | https://www.the-numbers.com/box-office-records/worldwide/all-movies/cumulative/released-in-2026 | Box-office tracker | 2026 worldwide ranking and grosses |
| S2 | https://www.the-numbers.com/box-office-records/worldwide/all-movies/cumulative/released-in-2025 | Box-office tracker | 2025 worldwide ranking and grosses |
| S3 | https://www.the-numbers.com/movie/Spider-Man-Brand-New-Day-(2026) | Box-office tracker | Release date 31 Jul 2026 (Sony); $360.1M opening; $2.52bn worldwide; extended-cut re-release 19 Nov 2026 |
| S4 | https://www.the-numbers.com/movie/Avengers-Doomsday-(2026) | Box-office tracker | Release date 18 Dec 2026 (Walt Disney); no box office yet |
| S5 | https://comicbookmovie.com/avengers/avengers-doomsday/avengers-doomsday-early-tracking-points-to-gargantuan-410-million-opening-weekend-a230990 | Fan/trade news | Mid-November tracking, about $410M opening |
| S6 | https://www.the-numbers.com/box-office-chart/weekend/2026/11/13 | Box-office tracker | Current US weekend (13–15 Nov 2026) |
| S7 | https://www.netflix.com/tudum/top10 | Official (Netflix) | Global Top 10 films, week 9–15 Nov 2026 (nothing usable; see Part 1c) |
| S8 | https://www.the-numbers.com/movie/Devil-Wears-Prada-2-The-(2026) | Box-office tracker | 1 May 2026, 20th Century Studios, comedy drama, $693.5M worldwide |
| S9 | https://www.the-numbers.com/movie/Scary-Movie-(2026) | Box-office tracker | 5 Jun 2026, Paramount, horror comedy, $231.3M worldwide |
| S10 | https://www.paramount.com/news/scary-movie-makes-a-killer-return-opening-to-franchise-record-1055-million-worldwide | Official studio | Franchise-record $105.5M worldwide opening |
| S11 | https://www.the-numbers.com/movie/Sheep-Detectives-The-(2026) | Box-office tracker | 8 May 2026, Amazon MGM, crime comedy, $133.1M worldwide |
| S12 | https://au.variety.com/2026/film/global/the-sheep-detectives-review-35973/ | Trade press | Review ("wholesomely offbeat family comedy") |
| S13 | https://screenanarchy.com/2026/05/the-sheep-detectives-review-hugh-jackman-leads-winning-ensemble-in-a-charming-family-oriented-murder.html | Review | Setting: English countryside fields, the village of Denbrook, rain |
| S14 | https://www.local10.com/business/2026/08/30/meep-meep-coyote-vs-acme-opens-with-155-million-spider-man-stays-no-1-for-fifth-weekend/ | AP wire | *Coyote vs. Acme* opened at $15.5M, No. 2, Ketchup Entertainment, live-action/animation hybrid |
| S15 | https://contrazoompod.com/reviews-coyote-vs-acme/ | Review | Desert-highway setting |
| S16 | https://www.hunantoday.cn/news/xhn/202602/31599355.html | Xinhua (state wire) | *Pegasus 3* topped the Spring Festival box office with 2.93bn yuan; Han Han's racing-comedy series |
| S17 | https://www.boxofficepro.com/?p=46471 | Trade press | *Focker-In-Law* Thanksgiving forecast ($30–40M three-day) |
| S18 | https://theplaylist.net/focker-in-law-trailer-ben-stiller-robert-de-niro-ariana-grande-bring-the-family-comedy-back-this-thanksgiving-20260415/ | Trade press | *Focker-In-Law*, Universal family comedy, Thanksgiving 2026 |
| S19 | https://scriptmag.com/spider-man-brand-new-day-review | Review | *Brand New Day* described as "grittier and more textured", with a dense, comic-like palette |
| S20 | https://dailybruin.com/2026/07/31/film-review-its-a-brand-new-day-for-spider-man-with-stunning-performances-cinematography/ | Review | Vibrant landscapes and colourful lighting in New York |
| S21 | https://www.dailycardinal.com/article/2026/08/destin-daniel-cretton-continues-his-marvel-hot-streak-with-spider-man-brand-new-day | Review | Shot of the hero looking over the city at sunrise |
| S22 | https://gizmodo.com/avengers-doomsday-trailer-in-lego-looks-much-brighter-than-the-original-2000802308 | News | Trailer palette: muted browns, yellows and greys, "almost like... magic hour" |
| S23 | https://theresonanceimpact.substack.com/p/for-i-am-doom | Fan essay | D23 trailer: a dark green palette that reads as regal |
| S24 | https://www.the-numbers.com/box-office-records/worldwide/all-movies/cumulative/released-in-2026 (same chart as S1; the film's own page was not opened) | Box-office tracker | *Supergirl* $126.5M worldwide, No. 37 of 2026 |
| S25 | https://www.cgmagonline.com/review/movie/supergirl-2026/ | Review | Mixed views of the look; dusty planet, grungy |
| S26 | https://comicbookmovie.com/supergirl/supergirl-review-kryptonite-to-anyone-who-likes-good-superhero-movies-a228529 | Review | "Mad Max-inspired" grungy look, often too dark |
| S27 | https://www.panavision.com/highlights/highlights-detail/the-cinematography-of-superman-with-henry-braham-bsc | Production partner | *Superman* (2025) cinematography by Henry Braham |
| S28 | https://www.notebookcheck.net/Superman-rendered-The-engineering-that-makes-James-Gunn-s-reboot-feel-new.1102060.0.html | News | VFX supervisor: "a lot of vibrant colors and a lot of saturation" |
| S29 | https://britishcinematographer.co.uk/jess-hall-asc-bsc-the-fantastic-four-first-steps/ | Trade (cinematographer) | Jess Hall: LUT palette of blue, green, orange and pale blue, from a 1968 comic |
| S30 | https://www.marvel.com/articles/movies/fantastic-four-first-steps-production-design-baxter-building-kasra-farahani | Official studio | Mid-century retro-futurist design, Apollo-era inspiration |
| S31 | https://filmmakermagazine.com/tag/thunderbolts | Trade press | DP Andrew Droz Palermo chose a muted palette |
| S32 | https://www.framerated.co.uk/thunderbolts-2025/ | Review | "Dreary and muted in color"; desert daylight looks drab |
| S33 | https://www.motionpictures.org/2025/02/red-alerts-cherry-blossom-brawls-with-captain-america-brave-new-world-production-designer-ramsey-avery/ | Industry body (MPA) | *Captain America: Brave New World*: cherry-blossom set pieces |
| S34 | https://www.panavision.com/highlights/highlights-detail/kramer-morgenthau-asc-on-the-cinematography-of-captain-america-brave-new-world | Production partner | 1970s paranoid-thriller look |
| S35 | https://www.laughingplace.com/disney-entertainment/zootopia-2-lightbox-expo-environment-design | Industry event coverage | Marsh Market: water-logged, chaotic and lively; Southeast Asian water markets |
| S36 | https://scroll.in/reel/1081575/ne-zha-2-review-a-spectacular-dazzling-feat-of-animation | Review | Fire and water motifs (Ne Zha / Ao Bing) |
| S37 | https://www.gamesradar.com/entertainment/anime-movies/demon-slayer-infinity-castle-has-broken-even-more-box-office-records-than-was-originally-projected-and-it-might-surpass-mugen-train-as-the-highest-grossing-anime-movie-ever/ | Trade/consumer press | *Infinity Castle* box-office records; Ufotable |
| S38 | https://d23.com/?p=153768 | Official (Disney D23) | *Elio*: purple marks the friendship; stained-glass colours of hope |
| S39 | https://abc7.com/post/pixars-new-animated-movie-elio-creates-colorful-worlds-tell-down-earth-story/16803187/ | News | Domee Shi: a "colorful, welcoming" Communiverse |
| S40 | https://www.walphs.com/2026/02/goat-movie-review-stephen-curry-sony-pictures.html | Review | *GOAT*: "neon-drenched fever dream", "metallic sheen" |
| S41 | https://www.creativebloq.com/art/animation/why-goat-will-look-different-from-other-animated-movies | Design press | *GOAT*: Sony stylised comic-book look |
| S42 | https://stanforddaily.com/2026/03/05/copy-ss-steph-curry-sport-comedy-goat/ | Review/news | *GOAT* opened to about $34M over Valentine's Day weekend |
| S43 | https://www.thestar.com.my/aseanplus/aseanplus-news/2025/08/30/is-chinese-animated-film-039nobody039-this-summers-039ne-zha-2039---its-story-and-art-style-explained | News | *Nobody*: ink-wash painting style, scroll-painting mist |
| S44 | https://pandaily.com/nobody-breaks-700-m-box-office-to-become-china-s-highest-grossing-domestic-2-d-animated-film | News | *Nobody*: China's top-grossing domestic 2D animated film |
| S45 | https://www.rogerebert.com/reviews/the-bad-guys-2-animated-film-review-2025 | Review | *The Bad Guys 2* (DreamWorks): painterly look |
| S46 | https://www.joblo.com/the-bad-guys-2-review/ | Review | Painterly textures, bold night lighting, space-station climax |

### 1b. Chosen titles (20)

| # | Title | Type | Studio / distributor | Why it counts as popular | Source |
|---|---|---|---|---|---|
| C1 | The Devil Wears Prada 2 | Live-action comedy (comedy drama) | 20th Century Studios | $693.5M worldwide, No. 6 of 2026 | S8, S1 |
| C2 | Scary Movie (2026) | Live-action horror-comedy parody | Paramount | $231.3M worldwide; biggest opening in the series ($105.5M worldwide) | S9, S10 |
| C3 | Pegasus 3 (飞驰人生3) | Live-action racing comedy (China) | Dir. Han Han; Chinese release (studio not checked) | Top film of the 2026 Spring Festival (2.93bn yuan); $641.1M worldwide, No. 8 of 2026 | S16, S1 |
| C4 | Coyote vs. Acme | Live-action/animation hybrid comedy | Ketchup Entertainment | $120.1M worldwide; opened at No. 2 behind *Spider-Man* to strong reviews | S14, S1 |
| C5 | The Sheep Detectives | Live-action crime comedy | Amazon MGM Studios | $133.1M worldwide; a surprise hit and well reviewed | S11, S12 |
| C6 | Focker-In-Law | Live-action family comedy | Universal | Opens Thanksgiving 2026 (25 or 26 Nov); forecast $30–40M three-day. It is about to open, so it is trending rather than proven | S17, S18 |
| A1 | Zootopia 2 | Animated | Walt Disney Animation | $1.87bn worldwide, No. 2 of 2025 | S2, S35 |
| A2 | Ne Zha 2 (哪吒之魔童闹海) | Animated (China) | Chinese studio (not checked) | $2.00bn worldwide, No. 1 of 2025 | S2, S36 |
| A3 | Demon Slayer: Infinity Castle | Animated (anime) | Ufotable (animation) | $793.5M worldwide, No. 7 of 2025; anime opening-weekend record | S2, S37 |
| A4 | Elio | Animated | Pixar | $154.3M worldwide (2025); Pixar original | S2, S38 |
| A5 | GOAT | Animated sports comedy | Sony Pictures Animation | $195.4M worldwide, No. 25 of 2026; opened to about $34M | S1, S42 |
| A6 | Nobody (浪浪山小妖怪) | Animated (China, 2D) | Shanghai Animation Film Studio | $218.4M worldwide; China's highest-grossing domestic 2D animated film | S2, S44 |
| A7 | The Bad Guys 2 | Animated heist comedy | DreamWorks Animation | $240.9M worldwide (2025) | S2, S45 |
| H1 | Spider-Man: Brand New Day | Superhero, street-level wall-crawler | Sony / Marvel | No. 1 film of 2026: $2.52bn worldwide, $360.1M opening; extended cut re-released 19 Nov 2026 | S3, S1 |
| H2 | Avengers: Doomsday | Superhero team-up | Marvel Studios / Disney | Opens 18 Dec 2026, not released yet; tracking about $410M opening | S4, S5 |
| H3 | Superman (2025) | Superhero | DC Studios / Warner Bros. | $618.7M worldwide, No. 10 of 2025 | S2, S27 |
| H4 | The Fantastic Four: First Steps | Superhero team | Marvel Studios | $521.9M worldwide (2025) | S2, S29 |
| H5 | Thunderbolts* | Superhero/antihero team-up | Marvel Studios | $382.4M worldwide (2025) | S2, S31 |
| H6 | Captain America: Brave New World | Superhero | Marvel Studios | $415.1M worldwide (2025) | S2, S33 |
| H7 | Supergirl | Superhero | DC Studios / Warner Bros. | $126.5M worldwide (2026). A soft result, but one of 2026's three superhero releases besides *Clayface* (excluded) | S1, S25 |

### 1c. What happened by 2026-11-20, and what could not be verified

- **_Spider-Man: Brand New Day_:** released 31 July 2026 and is the top film of 2026 (S3).
- **_Avengers: Doomsday_:**
  - It has not released; it is dated 18 December 2026 (S4).
  - Marvel's own film page (https://www.marvel.com/movies/avengers-doomsday) returned HTTP 403, so the date comes from The Numbers and trade coverage.
  - Its palette comes from trailer commentary (S22, S23), not from the finished film.
- **_Focker-In-Law_:**
  - It has not opened.
  - Sources disagree on 25 versus 26 November (S17, S18).
  - Its mood is wholly (inferred).
- **Studios not checked:** I did not verify the studios behind *Pegasus 3* and *Ne Zha 2*.
- **_Pegasus 3_ look:** I found no source on its visual style; Xinhua only says it is the series' "most pure racing film", so the palette is (inferred).
- **_Demon Slayer: Infinity Castle_ palette:** I found no source on its colours; the mood is (inferred) from the setting.
- **_Ne Zha 2_ in Variety:** the Variety review's wording on colour could not be confirmed; the fire/water contrast comes from Scroll (S36).
- **_Brand New Day_ filming:** a claim that it was partly shot in Glasgow appears in one review (S19) and was not confirmed elsewhere. It is not used.
- **_Supergirl_ figures:** the about $200M budget figure seen in one result is unverified and not used.
- **Netflix:** the global Top 10 for 9–15 Nov 2026 (S7) has no new, unexcluded comedy or superhero hit. Its entries are Christmas titles, *Sing*/*Sing 2* (excluded) and *A Dog's Perfect Christmas* (excluded), so no title was chosen from it.
- **Titles left out:**
  - Dropped: *How to Rob a Bank* (opened 13 Nov 2026, $5.1M, poorly reviewed).
  - Not used: *Jumanji: Open World* (25 Dec 2026), because it is not out and its title is reported by one source only.

---

## Part 2: Colour moods

| # | Title | Colour mood | Source |
|---|---|---|---|
| C1 | The Devil Wears Prada 2 | The Milan couture montage is the highlight; day and night looks; polished, glossy fashion editorial. Ivory, ink black and an oxblood-lipstick accent is (inferred) | Search summaries of reviews; e.g. https://www.joblo.com/the-devil-wears-prada-2-review/ ; palette (inferred) |
| C2 | Scary Movie (2026) | A horror parody that pairs slasher-night darkness with gross-out gags. Midnight purple-black with a goofy slime green is (inferred) | S9 (genre); palette (inferred) |
| C3 | Pegasus 3 | Racing comedy: asphalt at night, rally orange, sun-baked dust (inferred) | S16 (genre only); palette (inferred) |
| C4 | Coyote vs. Acme | A Looney Tunes desert, with miles of empty highway in the American desert; lush animation in live action. Sky-to-sand gradient and terracotta (inferred) | S15, S14 |
| C5 | The Sheep Detectives | English countryside, the fields where the flock lives, a village, a rainstorm. Wet greens and wool cream (inferred) | S13 |
| C6 | Focker-In-Law | Thanksgiving family comedy in a warm home: cranberry, pumpkin, butter cream (inferred) | S18 (genre/season); palette (inferred) |
| A1 | Zootopia 2 | Marsh Market: water-logged, chaotic, lively, full of comedic energy; based on Southeast Asian water markets and southern US ports. Swamp teal with lantern orange (inferred) | S35 |
| A2 | Ne Zha 2 | Fire against water/ice: Ne Zha designed as flame, Ao Bing in cool ice tones; monumental, ornate maximalism | S36; https://acas.world/2025/03/17/spectacular-digital-maximalism-some-thoughts-on-ne-zha-2/ |
| A3 | Demon Slayer: Infinity Castle | Shifting fortress of dark lacquered wood corridors, lantern amber, wisteria purple (inferred). Sources only say "breathtaking visuals" | S37; palette (inferred) |
| A4 | Elio | A glowing, light-emitting Communiverse; purple for the friendship; stained-glass colours of hope; "colorful, welcoming" | S38, S39 |
| A5 | GOAT | "Neon-drenched fever dream", "hyper-kinetic explosion of color", "metallic sheen"; stylised comic-book look | S40, S41 |
| A6 | Nobody | Traditional ink-wash painting, scroll-painting mountain mist, washes of pale ink. A cinnabar seal-red accent is (inferred) | S43 |
| A7 | The Bad Guys 2 | Painterly textures, bold night lighting, a car chase through Cairo, a space-station climax "absolutely ravishing". Midnight navy with getaway gold (inferred) | S45, S46 |
| H1 | Spider-Man: Brand New Day | Grittier and more textured than earlier films, a dense comic-like palette, colourful city lighting, the city at sunrise. Brick, steel and sunrise apricot (inferred); deliberately not the costume colours | S19, S20, S21 |
| H2 | Avengers: Doomsday | Trailers are "surprisingly muted", with browns, yellows and greys like magic hour; a dark regal green in the D23 trailer | S22, S23 |
| H3 | Superman (2025) | "A lot of vibrant colors and a lot of saturation", a very clean look, brighter and more hopeful than earlier takes. Clear sky blue and white with a teal accent (inferred); deliberately not the costume colours | S28, S27 |
| H4 | The Fantastic Four: First Steps | 1960s primary palette (blue, green, orange, pale blue) from a 1968 comic; mid-century retro-futurism; Apollo-era references | S29, S30 |
| H5 | Thunderbolts* | A muted palette chosen on purpose for the antiheroes; dreary and muted yet never dull; drab desert daylight; shadows close to the characters | S31, S32 |
| H6 | Captain America: Brave New World | Cherry blossoms at peak bloom in Washington; a 1970s paranoid-thriller look. Blossom pink with 70s olive (inferred); deliberately not flag colours | S33, S34 |
| H7 | Supergirl | Dusty alien planet, grungy and Mad Max-like, wide alien landscapes, often dark. Rust and ochre dusk (inferred) | S25, S26 |

---

## Part 3: The 20 palettes

All colours are ARGB hex. When a colour's alpha is below FF (the Glass keys), the contrast script composites it over each background end before measuring.

### Trade-dress and naming check

- **Names:** none of the names is a film title, character, studio, team or hero name, and none hints at one. I avoided "Doom", "Steel", "Void", "Sakura", "Blossom", "Ember", "Aurora", "Nebula", "Dune", "Rust" and "Lantern" because each is, or points to, an existing title or character.
- **Costume colours:** no palette uses a red-and-blue pair with dark lines, the hero costume colours (red/blue/yellow, the four-person blue suit, or a stars-and-stripes set), or Doom's green-plus-gunmetal.
  - Borough uses brick, steel and apricot.
  - Zenith uses sky, white and teal.
  - Atomic uses pale blue, cream, teal and orange.
  - Petal uses blossom pink and olive.
  - Sepia uses magic-hour gold rather than green.
- **No pictures:** no logos, emblems, web patterns or costume patterns are described or implied.

### 3a. Palette table

| # | Name | Group / inspired by (notes only) | Key style | isDark | Background (solid or top → bottom) | key | keyPressed | actionKey | label | actionLabel | hint | callout | accent | onAccent |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Atelier | Comedy: The Devil Wears Prada 2 (Milan couture, editorial ivory and ink) | Square | false | FFF4EFE6 | FFFFFFFF | FFE4DCCF | FF6E1F2E | FF1A1717 | FFFFFFFF | FF5E5650 | FFFFFFFF | FF8A2436 | FFFFFFFF |
| 2 | Gasp | Comedy: Scary Movie (slasher night with a silly slime green) | Raised | true | FF120E1A | FF241D30 | FF3A2F4C | FF9BE34A | FFF2EEF8 | FF101208 | FFB3A8C4 | FF2E2540 | FFB6F25A | FF141A06 |
| 3 | Rally | Comedy: Pegasus 3 (night asphalt, rally orange) | Borderless | true | FF16181B | FF1E2124 | FF2E3236 | FFF28C28 | FFF5F1EA | FF1A1208 | FFB9B2A6 | FF2A2D31 | FFFFA640 | FF1E1406 |
| 4 | Mesa | Comedy: Coyote vs. Acme (desert sky to sand, terracotta) | Rounded | false | FFBFE0F2 → FFF2D3A8 | FFFFF8EE | FFF1DFC6 | FFB4471E | FF2B1D14 | FFFFFFFF | FF6E5544 | FFFFFBF5 | FFA5401A | FFFFFFFF |
| 5 | Pasture | Comedy: The Sheep Detectives (wet fields, wool cream, moss) | Pill | false | FFE3EBD9 | FFFAF8F1 | FFE6E1D2 | FF3F6B3A | FF1F2A1C | FFFFFFFF | FF5A6455 | FFFFFFFF | FF3B6A35 | FFFFFFFF |
| 6 | Hearth | Comedy: Focker-In-Law (Thanksgiving cranberry and pumpkin) | Outline | false | FFFBF1E4 | FFF6E6D2 | FFEBD3B6 | FF9C3B1B | FF2E1A10 | FFFFFFFF | FF6F5240 | FFFFFFFF | FFB0441C | FFFFFFFF |
| 7 | Bayou | Animated: Zootopia 2 (Marsh Market swamp water, lantern orange) | Glass | true | FF0B3437 → FF062124 | 2EFFFFFF | 45FFFFFF | FFF2A541 | FFF4FAF7 | FF2A1604 | FFD0E6E2 | FF1C4E52 | FFF7B24E | FF2A1604 |
| 8 | Kiln | Animated: Ne Zha 2 (fire meeting ice) | Raised | true | FF5A1A0A → FF0B2233 | FF3A1F2A | FF55303E | FFE8452C | FFFFF1E8 | FF1A0503 | FFE0BFC0 | FF2A2440 | FF8FD3E8 | FF06192A |
| 9 | Lacquer | Animated: Demon Slayer: Infinity Castle (lacquered wood, lantern amber, wisteria) | Square | true | FF1A1012 | FF2B1C1E | FF43302F | FFB08BD6 | FFF6EBDD | FF1E1028 | FFC9B49E | FF352426 | FFE0A050 | FF241405 |
| 10 | Orbit | Animated: Elio (glowing purples, stained-glass hope) | Glass | true | FF2A1458 → FF0D0B2A | 2EFFFFFF | 4DFFFFFF | FF6FE3D2 | FFF6F0FF | FF0B2A27 | FFD2C6F0 | FF3A2470 | FFFFB6E1 | FF3A0A2A |
| 11 | Sherbet | Animated: GOAT (neon candy, metallic sheen) | Pill | true | FF140B24 | FF2A1A44 | FF41296A | FFFF5FA8 | FFFFF5FB | FF2A0618 | FFC9B8EA | FF33204F | FF3CF0C8 | FF04241D |
| 12 | Scroll | Animated: Nobody (ink wash on rice paper, cinnabar seal) | Borderless | false | FFF4F1E8 → FFE6E3D9 | FFEDEAE0 | FFDAD6CA | FF2E2F33 | FF1D1E21 | FFF4F1E8 | FF5C5E60 | FFFFFFFF | FFB23A2A | FFFFFFFF |
| 13 | Caper | Animated: The Bad Guys 2 (painterly night heist, getaway gold) | Outline | true | FF101A33 → FF070C1C | FF16213D | FF24335A | FFE8B84A | FFF3EEDF | FF1F1606 | FFB8BED2 | FF1D2A4C | FFF0C35A | FF241A04 |
| 14 | Borough | Superhero: Spider-Man: Brand New Day (brick, steel, city sunrise) | Rounded | true | FF4A2E2A → FF181A1F | FF2C2B30 | FF45403F | FFF4A56A | FFF8F1E9 | FF2A1308 | FFC8BDB4 | FF363238 | FFFFB37A | FF2A1308 |
| 15 | Sepia | Superhero: Avengers: Doomsday (muted magic-hour browns and golds) | Raised | true | FF1E1A14 | FF2F2A22 | FF463E31 | FFD9A441 | FFF3EADA | FF22180A | FFC2B6A0 | FF3A3428 | FFE2B455 | FF241906 |
| 16 | Zenith | Superhero: Superman (bright saturated optimism, clear sky) | Glass | false | FF8FD0F5 → FFE4F4FD | 99FFFFFF | 55FFFFFF | FF14507A | FF0D2238 | FFFFFFFF | FF2F4A60 | FFFFFFFF | FF0A6A60 | FFFFFFFF |
| 17 | Atomic | Superhero: The Fantastic Four: First Steps (mid-century pale blue, teal, orange) | Pill | false | FFD8E9EE | FFF7F3E8 | FFE5DED0 | FF2E7D74 | FF1E2A30 | FFFFFFFF | FF4E5B60 | FFFFFFFF | FFB84F1B | FFFFFFFF |
| 18 | Overcast | Superhero: Thunderbolts* (muted, desaturated shadow) | Borderless | true | FF1F2224 | FF26292C | FF35393D | FF8C9AA0 | FFE8E6E1 | FF15181A | FFA9ADAE | FF303438 | FFC9B79A | FF1B1712 |
| 19 | Sirocco | Superhero: Supergirl (dusty alien planet, rust and ochre) | Square | true | FF3B2418 → FF1A1412 | FF2E221C | FF48362C | FFD9824A | FFF6E9DC | FF22100A | FFCDB5A2 | FF3A2C24 | FFE89A5E | FF2A1408 |
| 20 | Petal | Superhero: Captain America: Brave New World (cherry blossom, 70s olive) | Outline | false | FFFBEFF1 | FFF8E4E8 | FFEFCFD6 | FF4A4A2E | FF2A1E22 | FFFFFFFF | FF6A545B | FFFFFFFF | FFB2476A | FFFFFFFF |

**Key styles:**

| Style | Count | Themes |
|---|---|---|
| Rounded | 2 | Mesa, Borough |
| Square | 3 | Atelier, Lacquer, Sirocco |
| Pill | 3 | Pasture, Sherbet, Atomic |
| Outline | 3 | Hearth, Caper, Petal |
| Raised | 3 | Gasp, Kiln, Sepia |
| Glass | 3 | Bayou, Orbit, Zenith |
| Borderless | 3 | Rally, Scroll, Overcast |

isDark: 12 dark, 8 light.

**Notes for implementation:**
- **Glass keys:** the Glass keys (Bayou, Orbit, Zenith) use translucent ARGB key and keyPressed colours. The contrast figures assume they are drawn straight onto the background gradient.
- **Outline and Borderless keys:** in Outline and Borderless styles, the `key` value is the faint fill or stroke tint, and the label was also checked against the bare background.

### 3b. Contrast results

These come from `contrast_check.py` (WCAG 2.x relative luminance, standard library only). Each ratio is the worst case across both gradient ends.

**Rules:**
- The label must reach 4.5:1 against the key, keyPressed and callout.
- actionLabel against actionKey, hint against key, and onAccent against accent must each reach 4.5:1.
- The accent must reach 3:1 against the background.
- For Outline, Glass and Borderless styles only, the label must also reach 4.5:1 against the background (the "label/bg (low-fill)" column).

The "Lowest 4.5-rule ratio" column is the theme's minimum across every 4.5:1 rule.

| # | Theme | Style | label/key | label/pressed | label/callout | actionLabel | hint/key | onAccent | accent/bg (>=3) | label/bg (low-fill) | Lowest 4.5-rule ratio | Pass |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Atelier | Square | 17.81 | 13.09 | 17.81 | 11.06 | 7.18 | 8.80 | 7.68 | n/a | 7.18 | yes |
| 2 | Gasp | Raised | 14.20 | 10.86 | 12.62 | 12.12 | 7.20 | 13.42 | 14.35 | n/a | 7.20 | yes |
| 3 | Rally | Borderless | 14.37 | 11.47 | 12.29 | 7.55 | 7.69 | 9.31 | 9.13 | 15.80 | 7.55 | yes |
| 4 | Mesa | Rounded | 15.45 | 12.50 | 15.81 | 5.44 | 6.54 | 6.27 | 4.38 | n/a | 5.44 | yes |
| 5 | Pasture | Pill | 14.05 | 11.42 | 14.93 | 6.22 | 5.83 | 6.37 | 5.20 | n/a | 5.83 | yes |
| 6 | Hearth | Outline | 13.50 | 11.42 | 16.52 | 6.87 | 5.81 | 5.69 | 5.10 | 14.79 | 5.69 | yes |
| 7 | Bayou | Glass | 7.24 | 5.46 | 8.79 | 8.42 | 5.87 | 9.41 | 7.33 | 12.73 | 5.46 | yes |
| 8 | Kiln | Raised | 13.48 | 10.11 | 13.33 | 4.99 | 8.79 | 10.72 | 7.97 | n/a | 4.99 | yes |
| 9 | Lacquer | Square | 13.86 | 10.48 | 12.46 | 6.47 | 8.16 | 7.91 | 8.27 | n/a | 6.47 | yes |
| 10 | Orbit | Glass | 8.21 | 5.42 | 11.30 | 9.89 | 5.71 | 10.37 | 9.73 | 14.06 | 5.42 | yes |
| 11 | Sherbet | Pill | 14.82 | 11.26 | 13.49 | 6.55 | 8.67 | 11.36 | 13.14 | n/a | 6.55 | yes |
| 12 | Scroll | Borderless | 13.85 | 11.47 | 16.67 | 11.84 | 5.41 | 5.95 | 4.63 | 12.98 | 5.41 | yes |
| 13 | Caper | Outline | 13.73 | 10.67 | 12.17 | 9.69 | 8.59 | 10.34 | 10.40 | 14.87 | 8.59 | yes |
| 14 | Borough | Rounded | 12.54 | 9.11 | 11.23 | 8.73 | 7.62 | 10.03 | 6.98 | n/a | 7.62 | yes |
| 15 | Sepia | Raised | 11.93 | 8.82 | 10.34 | 7.76 | 7.11 | 8.95 | 8.98 | n/a | 7.11 | yes |
| 16 | Zenith | Glass | 13.15 | 11.48 | 16.12 | 8.54 | 7.55 | 6.47 | 3.86 | 9.60 | 6.47 | yes |
| 17 | Atomic | Pill | 13.25 | 10.98 | 14.69 | 4.88 | 6.34 | 5.03 | 4.03 | n/a | 4.88 | yes |
| 18 | Overcast | Borderless | 11.73 | 9.33 | 10.06 | 6.15 | 6.46 | 9.10 | 8.17 | 12.83 | 6.15 | yes |
| 19 | Sirocco | Square | 12.93 | 9.57 | 11.25 | 6.32 | 7.88 | 7.67 | 6.35 | n/a | 6.32 | yes |
| 20 | Petal | Outline | 13.20 | 11.14 | 16.06 | 9.09 | 5.69 | 5.25 | 4.68 | 14.32 | 5.25 | yes |

All 20 pass.

**Tightest margins:**
- 4.5:1 rules:
  - Atomic actionLabel: 4.88
  - Kiln actionLabel: 4.99
  - Atomic onAccent: 5.03
- 3:1 rule (accent/bg):
  - Zenith: 3.86
  - Atomic: 4.03

**Revision:** one revision was needed. In the first draft, Bayou's pressed-key label was 4.15:1, a fail. The fix:
- darkened the gradient top to FF0B3437
- lowered the key alpha to 2E and the pressed alpha to 45
- brightened the label to FFF4FAF7 and the hint to FFD0E6E2

## Part 4: What changed when the palettes went into Theme.kt

- **Prism became Orbit**, because a trending theme is already called Prism.
- **Glass keys are opaque in Theme.kt.** The keyboard itself draws a Glass key at 55% over the background, and a user can pick any key style for any theme, so every key colour must also work as a solid key.
  - Zenith's see-through key is now that colour drawn over the middle of its gradient, made as light as the 55% glass allows.
  - Bayou's and Orbit's keys are now their see-through colours as they show over the middle of the gradient, so the hint keeps 4.5:1 on a solid key.
  - The three themes' pressed keys are their see-through pressed colours over the middle of the gradient.
- **ThemesTest is now the check.** It holds all fifty themes to the floors in every key style, at the top, middle and bottom of a gradient. It replaces the script this research used.
