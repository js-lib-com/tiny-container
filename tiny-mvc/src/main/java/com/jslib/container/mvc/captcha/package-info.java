/**
 * Server logic for Simple CAPTCHA based on set of images. This CAPTCHA implementation tries to find 
 * a balance between easy to use and effective security. And it is indeed simple and fun to use but
 * provided protection is not so effective as of, for example, Google reCAPTCHA. Anyway, for usual boots,
 * not equipped with image recognition based on neural network this implementation is still effective.
 * A solution may be to distort the image but we are back to original hard to use solution.
 * <p>
 * Simple CAPTCHA is designed to work with <code>captcha</code> widget from script library, see 
 * <a href="http://api.js-lib.com/widget/js/widget/Captcha.html">widget API</a>.   
 *
 * @author Iulian Rotaru
 */
package com.jslib.container.mvc.captcha;

