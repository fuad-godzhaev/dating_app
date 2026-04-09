//go:build tools

// Keeps golang.org/x/mobile in the module graph so `gomobile bind` (which
// generates code importing golang.org/x/mobile/bind) resolves it. Not compiled
// into the binding; the `tools` build tag is never enabled for a normal build.
package golibp2p

import _ "golang.org/x/mobile/bind"
