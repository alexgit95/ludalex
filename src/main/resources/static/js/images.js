function repliImage(image) {
    const secours = image.getAttribute('data-secours');
    image.removeAttribute('data-secours');
    if (secours) {
        image.src = secours;
        return;
    }
    image.onerror = null;
    if (image.hasAttribute('data-retirer')) {
        image.remove();
    } else {
        image.src = '/icons/defaut.svg';
    }
}