package io.structify.domain.db.reflection

fun setPrivateProperty(instance: Any, propertyName: String, value: Any?) {
	val property = instance::class.java.getDeclaredField(propertyName)
	val isAccessible = property.canAccess(instance)
	property.isAccessible = true
	property.set(instance, value)
	property.isAccessible = isAccessible
}
