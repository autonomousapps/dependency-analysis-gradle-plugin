package com.autonomousapps.kit

final class AndroidSubproject extends Subproject {

  final AndroidManifest manifest
  final AndroidStyleRes styles
  final AndroidColorRes colors
  final List<AndroidLayout> layouts

  AndroidSubproject(
    String name,
    BuildScript buildScript,
    List<Source> sources,
    AndroidManifest manifest = AndroidManifest.DEFAULT_MANIFEST,
    AndroidStyleRes styles = AndroidStyleRes.DEFAULT,
    AndroidColorRes colors = AndroidColorRes.DEFAULT_COLORS_XML,
    List<AndroidLayout> layouts = []
  ) {
    super(name, buildScript, sources)
    this.manifest = manifest
    this.styles = styles
    this.colors = colors
    this.layouts = layouts
  }
}
