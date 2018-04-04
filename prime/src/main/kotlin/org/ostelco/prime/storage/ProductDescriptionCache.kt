package org.ostelco.prime.storage


import org.ostelco.prime.storage.entities.Product

interface ProductDescriptionCache {

    fun addTopupProduct(sku: String, noOfBytes: Long)

    fun isValidSKU(sku: String): Boolean

    fun getProductForSku(sku: String): Product?
}