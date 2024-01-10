package org.totschnig.onedrive

import com.microsoft.graph.requests.DriveItemCollectionPage

fun DriveItemCollectionPage.getAll() = buildList {
    var currentPage: DriveItemCollectionPage? = this@getAll
    while (currentPage != null) {
        addAll(currentPage.currentPage)
        // Get the next page
        val nextPage = currentPage.nextPage
        if (nextPage == null) {
            break
        } else {
            currentPage = nextPage.buildRequest().get()
        }
    }
}