
import arrow.core.Either
import arrow.core.fix
import arrow.effects.IO
import arrow.instances.either.monad.monad
import org.ostelco.prime.dsl.WriteTransaction
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.storage.StoreError
import org.ostelco.prime.storage.graph.Neo4jStoreSingleton.applyProduct
import org.ostelco.prime.storage.graph.Neo4jStoreSingleton.createPurchaseRecordRelation
import org.ostelco.prime.storage.graph.OnNewCustomerAction
import org.ostelco.prime.storage.graph.PrimeTransaction
import java.time.Instant
import java.util.*

object : OnNewCustomerAction {
    override fun apply(identity: Identity,
                       customerId: String,
                       transaction: PrimeTransaction): Either<StoreError, Unit> {

        val welcomePackProductSku = "2GB_FREE_ON_JOINING"

        return IO {
            Either.monad<StoreError>().binding {
                WriteTransaction(transaction).apply {
                    val product = get(Product::class, welcomePackProductSku).bind()
                    createPurchaseRecordRelation(
                            customerId,
                            PurchaseRecord(
                                    id = UUID.randomUUID().toString(),
                                    product = product,
                                    timestamp = Instant.now().toEpochMilli()
                            )
                    ).bind()
                    applyProduct(
                            customerId = customerId,
                            product = product
                    ).bind()
                }
                Unit
            }.fix()
        }.unsafeRunSync()
    }
}