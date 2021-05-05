package com.udacity.catpoint.image.service;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Service that tries to guess if an image displays a cat.
 */
public class FakeImageService {
    private final Random r = new Random();

    public boolean imageContainsCat(BufferedImage image, float confidenceThreshold) {
        return r.nextBoolean();
    }
}
