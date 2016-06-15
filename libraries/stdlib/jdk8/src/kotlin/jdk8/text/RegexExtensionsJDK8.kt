@file:JvmName("RegexExtensionsJDK8Kt")
package kotlin.jdk8.text


public operator fun MatchGroupCollection.get(name: String): MatchGroup? {
    val namedGroups = this as? MatchNamedGroupCollection ?:
            throw UnsupportedOperationException("Retrieving groups by name is not supported on this platform.")

    return namedGroups[name]
}
