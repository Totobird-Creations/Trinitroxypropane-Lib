package net.totobirdcreations.gaslib.util


data class RGBA(
    val r : Float,
    val g : Float,
    val b : Float,
    val a : Float
) {

    fun withAlpha(a : Float) : RGBA {
        return RGBA(this.r, this.g, this.b, a);
    }

    fun mul(v : Float) : RGBA {
        return RGBA(
            (this.r * v).coerceIn(0.0f, 1.0f),
            (this.g * v).coerceIn(0.0f, 1.0f),
            (this.b * v).coerceIn(0.0f, 1.0f),
            this.a
        )
    }

    fun mix(other : RGBA) : RGBA {
        return RGBA(
            (this.r + other.r) / 2.0f,
            (this.g + other.g) / 2.0f,
            (this.b + other.b) / 2.0f,
            1.0f - (1.0f - this.a) * (1.0f - this.b)
        );
    }

    @Suppress("unused")
    companion object {

        val WHITE : RGBA = RGBA(1.0f, 1.0f, 1.0f, 1.0f);

        val BLACK : RGBA = RGBA(0.0f, 0.0f, 0.0f, 1.0f);

        val RED : RGBA = RGBA(1.0f, 0.0f, 0.0f, 1.0f);

        val ORANGE : RGBA = RGBA(1.0f, 0.5f, 0.0f, 1.0f);

        val YELLOW : RGBA = RGBA(1.0f, 1.0f, 0.0f, 1.0f);

        val LIME : RGBA = RGBA(0.5f, 1.0f, 0.0f, 1.0f);

        val GREEN : RGBA = RGBA(0.0f, 1.0f, 0.0f, 1.0f);

        val CYAN : RGBA = RGBA(0.0f, 1.0f, 1.0f, 1.0f);

        val BLUE : RGBA = RGBA(0.0f, 0.5f, 1.0f, 1.0f);

        val DARK_BLUE : RGBA = RGBA(0.0f, 0.0f, 1.0f, 1.0f);

        val PURPLE : RGBA = RGBA(0.5f, 0.0f, 1.0f, 1.0f);

        val PINK : RGBA = RGBA(1.0f, 0.0f, 0.75f, 1.0f);

    }

}