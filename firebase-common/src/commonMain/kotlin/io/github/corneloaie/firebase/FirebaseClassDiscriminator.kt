package io.github.corneloaie.firebase

import kotlinx.serialization.InheritableSerialInfo

@InheritableSerialInfo
@Target(AnnotationTarget.CLASS)
public annotation class FirebaseClassDiscriminator(val discriminator: String)
