response:
  role: "Stockboard"
  status: "acknowledged"
  action: "provide_news_filtering_architecture"
  message: "我完全理解你的需求。為了讓畫面上的新聞能精準如圖中分類到「財經新聞」、「台股盤勢」、「美股」、「ETF」、「黃金」等分頁，避免無關的雜訊新聞，我們需要將原本混雜的 Yahoo 總新聞來源進行客戶端過濾（Client-side filtering），並將此邏輯與你的 Jetpack Compose UI 綁定。"
  architecture_plan:
    step_1_define_categories:
      file: "app/src/main/java/com/stockboard/ui/news/NewsCategory.kt"
      concept: "建立一個帶有過濾關鍵字的 Enum，對應你圖片上的 Tab 標籤。"
    step_2_viewmodel_filtering:
      file: "app/src/main/java/com/stockboard/viewmodel/NewsViewModel.kt"
      concept: "結合 Kotlin Coroutines 的 `combine`，當使用者切換分類 Tab 時，利用特定關鍵字即時從 Room Database撈出的資料中進行篩選。"
    step_3_ui_integration:
      file: "app/src/main/java/com/stockboard/ui/news/NewsScreen.kt"
      concept: "實作 Jetpack Compose 的 `ScrollableTabRow`，並動態渲染過濾後的新聞列表。"
  code_implementation:
    enum_class: |
      enum class NewsCategory(val displayName: String, val keywords: List<String>) {
          ALL("全部", emptyList()),
          FINANCE("財經新聞", listOf("財經", "經濟", "央行", "降息", "升息")),
          TW_STOCK("台股盤勢", listOf("台股", "盤勢", "大盤", "台積電", "權值股")),
          US_STOCK("美股", listOf("美股", "道瓊", "那斯達克", "標普", "費半", "美聯準")),
          ETF("ETF", listOf("ETF", "高股息", "成分股", "配息")),
          GOLD("黃金", listOf("黃金", "金價", "避險", "金市"))
      }
    viewmodel_logic: |
      // 在 NewsViewModel 中加入以下狀態管理
      private val _selectedCategory = MutableStateFlow(NewsCategory.ALL)
      val selectedCategory: StateFlow<NewsCategory> = _selectedCategory.asStateFlow()

      // 假設 newsDao.getAllNews() 回傳的是 Flow<List<NewsEntity>>
      val displayNews: Flow<List<NewsEntity>> = combine(
          newsDao.getAllNews(),
          _selectedCategory
      ) { newsList, category ->
          if (category == NewsCategory.ALL) {
              newsList
          } else {
              newsList.filter { news ->
                  category.keywords.any { keyword ->
                      news.title.contains(keyword, ignoreCase = true) ||
                      news.description.contains(keyword, ignoreCase = true)
                  }
              }
          }
      }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

      fun setCategory(category: NewsCategory) {
          _selectedCategory.value = category
      }
  technical_note: "由於 Yahoo 很多細分的 RSS 連結已經漸漸合併為綜合大類（例如單一的財經 RSS），因此透過 `StockUpdateWorker.kt` 或 `NewsRssParser.kt` 將全量資料抓取回本地端並存入 Room 資料庫後，再由 ViewModel 層利用關鍵字比對（Contains）進行即時分類，是目前最彈性且不會造成網路重複呼叫浪費的最佳實踐方案。"