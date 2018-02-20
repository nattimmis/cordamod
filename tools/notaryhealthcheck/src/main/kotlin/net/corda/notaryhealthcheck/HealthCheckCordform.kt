package net.corda.notaryhealthcheck

import net.corda.cordform.CordformContext
import net.corda.cordform.CordformDefinition
import net.corda.cordform.CordformNode
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.node.services.Permissions.Companion.all
import net.corda.node.services.config.NotaryConfig
import net.corda.node.services.config.RaftConfig
import net.corda.nodeapi.internal.DevIdentityGenerator
import net.corda.testing.node.User
import net.corda.testing.node.internal.demorun.*
import java.nio.file.Paths

fun main(args: Array<String>) = HealthCheckCordform().nodeRunner().deployAndRunNodes()

class HealthCheckCordform : CordformDefinition() {
    private fun createNotaryNames(clusterSize: Int) = (0 until clusterSize).map { CordaX500Name("Notary Service $it", "Zurich", "CH") }
    private val notaryDemoUser = User("demou", "demop", setOf(all()))
    private val notaryNames = createNotaryNames(3)
    private val clusterName = CordaX500Name("Raft", "Zurich", "CH")

    init {
        nodesDirectory = Paths.get("build", "nodes")
        fun notaryNode(index: Int, nodePort: Int, clusterPort: Int? = null, configure: CordformNode.() -> Unit) = node {
            name(notaryNames[index])
            val clusterAddresses = if (clusterPort != null) listOf(NetworkHostAndPort("localhost", clusterPort)) else emptyList()
            notary(NotaryConfig(validating = true, raft = RaftConfig(NetworkHostAndPort("localhost", nodePort), clusterAddresses)))
            configure()
        }
        notaryNode(0, 10008) {
            p2pPort(10009)
            rpcPort(10010)
        }
        notaryNode(1, 10012, 10008) {
            p2pPort(10013)
            rpcPort(10014)
        }
        notaryNode(2, 10016, 10008) {
            p2pPort(10017)
            rpcPort(10018)
        }
        node {
            name(CordaX500Name("R3 Notary Health Check", "London", "GB"))
            p2pPort(10002)
            rpcPort(10003)
            rpcUsers(notaryDemoUser)
        }
    }

    override fun setup(context: CordformContext) {
        DevIdentityGenerator.generateDistributedNotarySingularIdentity(
                notaryNames.map { context.baseDirectory(it.toString()) },
                clusterName
        )
    }
}