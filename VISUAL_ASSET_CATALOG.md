# ONE MOVE — Official Visual Asset & Procedural Component Catalog
**Art Direction: Midnight Kinetic Workshop**

---

## 1. Procedural Renderers Index

All production game assets are procedurally rendered in vector format with hardware acceleration in Android Jetpack Compose (`androidx.compose.ui.graphics.drawscope.DrawScope`).

| Renderer Name | Package / Location | Visual Responsibilities |
|---|---|---|
| `ToyBoxBackgroundRenderer` | `com.example.onemove.ui.render` | Renders the precision-molded midnight composite chassis, peg socket grid, panel seams, satin brass perimeter rim, and corner assembly screws. |
| `RailRenderer` | `com.example.onemove.ui.render` | Skins arbitrary physical platforms into brushed dark titanium rails with soft cast shadows, dark underside, specular top edge, and satin brass rivet caps. |
| `MechanicalJointRenderer` | `com.example.onemove.ui.render` | Renders rotating seesaw levers, triangular titanium fulcrum stands, large satin brass center axle bearings, and creature gate latch assemblies. |
| `PinRenderer` | `com.example.onemove.ui.render` | Renders satin brass pull ring handles, dark titanium shafts, recessed socket collars, and distinct tactical geometric emblems (A=Diamond, B=Square, C=Circle, D=Triangle). |
| `HeroCreatureRenderer` | `com.example.onemove.ui.render` | Renders Pip (cat ears), Mochi (antennae), and Blobbo (sprout) with squash & stretch, motion trails, ground shadows, and 6 expressive facial states. |
| `HomeNestRenderer` | `com.example.onemove.ui.render` | Renders the arched dark walnut shelter canopy, warm amber sanctuary interior glow, plush green velvet receiving sofa bed, golden cat mascot crest, and 3 creature docking LED wells. |
| `DangerBasinRenderer` | `com.example.onemove.ui.render` | Renders the recessed dark composite dead-end trap basin with bottom coral magma containment glow, coral rim border, and muted warning triangle emblem (no gore). |
| `SpringBumperRenderer` | `com.example.onemove.ui.render` | Renders machined steel mounting plate, dynamically coiled steel spring wire responding to physical compression, and kinetic cyan strike piston head. |
| `WreckerAndStoneRenderer` | `com.example.onemove.ui.render` | Renders heavy cast-iron wrecker ball (with satin brass belt & mascot crest) and the chiseled granite rolling stone boulder. |
| `KineticParticleRenderer` | `com.example.onemove.ui.render` | Renders dust poofs, metal sparks, confetti bursts, celebration stars, and rescue nest arrival glow hearts. |

---

## 2. Asset Details & Parameter Specifications

### Asset 01: Midnight Machine Chassis (`ToyBoxBackgroundRenderer`)
- **Dimensions**: Full World Bounds (1000px x 1600px).
- **Layers**:
  1. Base Radial Vignette: Midnight Slate (`#0F172A`) to Deepest Midnight (`#0A0F1D`) and Deep Void (`#020617`).
  2. Outer Chassis Bevel Frame: Molded Slate (`#1E293B`).
  3. Satin Brass Perimeter Inset Rim: `#D97706` satin brass accent line.
  4. Modular Peg Grid: 80px dot grid (`#22475569`).
  5. Recessed Panel Seam Grooves: Upper and lower horizontal seam lines.
  6. 4 Machined Satin Brass Corner Screws: Brass heads with cross-slotted socket slots and specular glints.
- **Physics Impact**: Zero physics impact (pure procedural canvas background).

### Asset 02: Structural Titanium Rails (`RailRenderer`)
- **Parameters**: `Platform(start, end, thickness, color, isBouncy)`.
- **Layers**:
  1. 3D Drop Shadow: Offset +7px Y, color `#66000000`.
  2. Dark Structural Underside: Width = `thickness + 2.5px`, color `#141B2D`.
  3. Brushed Dark Titanium Body: Width = `thickness`, color `#334155` (or `#EF4444` for hazard, `#0EA5E9` for active).
  4. Specular Top Highlight Edge: Width = `3.5px`, color `#94A3B8`.
  5. Satin Brass Endpoint Rivet Caps: Brass housing (`#78350F`) with brass pin core (`#F59E0B`) and specular glint.
- **Physics Impact**: Skinning over immutable physics capsule segments.

### Asset 03: Mechanical Seesaw & Gates (`MechanicalJointRenderer`)
- **Components**:
  - Triangular Fulcrum Stand: Molded titanium stand (`#1E293B`) with beveled border (`#334155`).
  - Central Axle Bearing: Concentric satin brass rings (`#B45309`, `#F59E0B`, `#FEF3C7`).
  - Seesaw Beam: Dark titanium lever with metallic top highlight and end weight cups.
  - Creature Gate: Moving arm with brass hinge pivot and end latch collar.

### Asset 04: Tactical Pins (`PinRenderer`)
- **Hierarchy**:
  - **PIN A**: **DIAMOND** handle emblem (Satin brass ring, diamond shape cutout).
  - **PIN B**: **SQUARE** handle emblem (Satin brass ring, square shape cutout).
  - **PIN C**: **CIRCLE** handle emblem (Satin brass ring, circle shape cutout).
  - **PIN D**: **TRIANGLE** handle emblem (Satin brass ring, triangle shape cutout).
- **Tactile Material**: Satin brass outer pull ring (`#F59E0B`), dark titanium shaft (`#334155`), and fine specular highlight.
- **Accessibility**: Guaranteed 48dp+ touch targets on handles and rods; distinct geometric silhouettes for color-blind accessibility.

### Asset 05: Hero Mascots (`HeroCreatureRenderer`)
- **Pip (Amber Mascot)**: Pointed cat ears with pink inner flap, cream belly patch, paw marks, curious pupils.
- **Mochi (Mint Mascot)**: Taller teardrop/jelly silhouette, bobbly antennae with glossy bulb tips, cute green freckles.
- **Blobbo (Coral Mascot)**: Plump bean silhouette, two-leaf green head sprout, golden star cheek highlights.
- **Hero Scale**: Visual radius = `physicsRadius * 1.25f` for crisp mobile rendering.
- **Expressive States**: NORMAL, CURIOUS, SURPRISED, DIZZY, HAPPY, DISAPPOINTED.

### Asset 06: Home Nest Sanctuary (`HomeNestRenderer`)
- **Parameters**: `GoalZone(center, radius)`.
- **Visuals**:
  - Warm Ambient Radial Glow: Soft radiating amber sanctuary illumination (`#F59E0B`).
  - Arched Wood Shelter Canopy: Dark walnut & warm amber wood grain (`#451A03` / `#78350F`) with satin brass roof trim.
  - Cozy Alcove Interior: Warm amber shaded depth gradient.
  - Plush Velvet Green Couch: Cushioned sofa bed (`#064E3B` / `#10B981`) with tufting buttons.
  - Golden Mascot Cat Crest: Gable victory emblem in satin gold (`#FBBF24`).
  - 3 Docking LED Status Wells: Dedicated docking wells for Pip, Mochi, Blobbo that illuminate upon arrival.

### Asset 07: Danger Basin Dead-End Trap (`DangerBasinRenderer`)
- **Parameters**: `DangerPit(bounds)`.
- **Visuals**:
  - Recessed Basin: Dark gradient (`#180505` to `#0D0202`).
  - Magma Floor Glow: Soft coral bottom containment illumination (`#66EF4444`).
  - Warning Striping: Alternating coral `#EF4444` and amber `#F59E0B` top lip segments.
  - Caution Emblem: Muted hazard triangle silhouette with exclamation mark.

### Asset 08: Spring Bumper Launcher (`SpringBumperRenderer`)
- **Components**: Steel mounting base with brass fasteners, 4-coil steel spring wire responding to physical compression, and kinetic cyan strike piston head (`#0EA5E9`).

### Asset 09: Heavy Wrecker Ball & Rolling Stone (`WreckerAndStoneRenderer`)
- **Heavy Ball**: Cast iron sphere gradient, satin brass equator belt with rivets, center mascot crest.
- **Rolling Stone**: Chiseled granite boulder gradient with crater facets.
