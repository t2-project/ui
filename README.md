# UI Service

This service is the UI of the T2-Project.
It is based on the UI of the original [TeaStore](https://github.com/DescartesResearch/TeaStore).

## Build and Run

Refer to the [Documentation](https://t2-documentation.readthedocs.io/en/latest/guides/deploy.html) on how to build, run or deploy the T2-Project services.

## Usage

Go to `<your-ui-url>/ui/products` to see all products.

If the product name or description is too long, the view might be a bit chopped up, but don't mind that.

Use the tiny arrow to set the number of units you want to add to your cart (you can not decrease them below zero) and press the "add" button to add that product to your cart.

Click the tiny shopping cart in the top right corner to go to your cart.

In this view, you may remove products from your cart (garbage can icon) or confirm your order ("confirm" button).

After confirmation you must insert payment details to finally place your order.

After placing the order you are a new person an may start putting products in to your shopping cart again.

## Application Properties

| property | read from env var | description |
| -------- | ----------------- | ----------- |
| t2.uibackend.url | T2_UIBACKEND_URL | url of the UI Backend |
