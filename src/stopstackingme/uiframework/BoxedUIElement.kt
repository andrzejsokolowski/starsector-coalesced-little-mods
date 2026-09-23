package stopstackingme.uiframework

import com.fs.starfarer.api.ui.UIComponentAPI

// Copyright Starficz, Licensed under LGPL-3.0-only
//
// The single declaration [ReflectionUtils] needs out of Starficz's UIExtensions.kt. Kept here,
// with the upstream signature, so the vendored ReflectionUtils.kt stays a byte-for-byte copy
// (bar its package line) and can be refreshed from upstream without edits.
abstract class BoxedUIElement(val boxedElement: UIComponentAPI)
