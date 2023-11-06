package de.unistuttgart.t2.ui;

import java.net.URI;
import java.util.*;

import javax.servlet.http.HttpSession;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import de.unistuttgart.t2.common.*;
import de.unistuttgart.t2.ui.domain.*;

/**
 * Defines the http enpoints of the UI.
 *
 * @author maumau
 */
@Controller
public class UIController {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private RestTemplate template;

    private final String urlProductsAll;
    private final String urlCart;
    private final String urlConfirm;

    public UIController(@Value("${t2.uibackend.url}") String urlUiBackend) {
        urlProductsAll = urlUiBackend + "products/";
        urlCart = urlUiBackend + "cart/";
        urlConfirm = urlUiBackend + "confirm/";
    }

    ////// PAGES TO REALLY LOOK AT ///////////

    @GetMapping("/ui/")
    public String index(Model model) {
        model.addAttribute("title", "T2-Project");
        return "index";
    }

    @GetMapping("/ui/products")
    public String products(Model model) {
        model.addAttribute("title", "Products");
        model.addAttribute("item", new ItemToAdd());

        List<Product> products = template.exchange(urlProductsAll, HttpMethod.GET, HttpEntity.EMPTY,
            new ParameterizedTypeReference<List<Product>>() {}).getBody();

        model.addAttribute("productslist", products);

        return "category";
    }

    @GetMapping("/ui/cart")
    public String cart(Model model, HttpSession session) {
        model.addAttribute("title", "Cart");
        model.addAttribute("item", new ItemToAdd());

        // Request and send
        RequestEntity<Void> request = new RequestEntity<>(HttpMethod.GET, URI.create(urlCart + session.getId()));
        ResponseEntity<List<Product>> response =
            template.exchange(request, new ParameterizedTypeReference<List<Product>>() {});

        // Set View
        List<Product> products = response.getBody();
        model.addAttribute("OrderItems", products);

        return "cart";
    }

    @GetMapping("/ui/confirm")
    public String confirm(Model model, HttpSession session) {

        model.addAttribute("title", "Confirm");
        model.addAttribute("details", new PaymentDetails());

        return "order";
    }

    ////////// ACTIONS /////////////

    @PostMapping("/ui/add")
    public String add(@ModelAttribute("item") ItemToAdd item, HttpSession session) {

        LOG.debug("Add item to card: {} | SessionID: {}", item.toString(), session.getId());

        // Body
        UpdateCartRequest body = new UpdateCartRequest(Map.of(item.getProductId(), item.getUnits()));
        RequestEntity<UpdateCartRequest> request =
            new RequestEntity<>(body, HttpMethod.POST, URI.create(urlCart + session.getId()));

        // Request and send
        ResponseEntity<List<Product>> response =
            template.exchange(request, new ParameterizedTypeReference<List<Product>>() {});

        LOG.info(response.getBody().toString());

        return "product";
    }

    @PostMapping("/ui/delete")
    public RedirectView delete(@ModelAttribute("item") ItemToAdd item, RedirectAttributes redirectAttributes,
        HttpSession session) {

        LOG.debug("Delete item from card: {} | SessionID: {}", item.toString(), session.getId());

        // Body
        UpdateCartRequest body = new UpdateCartRequest(Map.of(item.getProductId(), -1 * item.getUnits()));
        template.postForEntity(URI.create(urlCart + session.getId()), body, Void.class);

        // TODO redirect : to display deleted products

        return new RedirectView("/ui/cart", true);
    }

    @PostMapping("/ui/confirm")
    public String confirm(@ModelAttribute("details") PaymentDetails details, Model model, HttpSession session) {

        LOG.debug("Confirm order | SessionID: {}", session.getId());

        // Body
        OrderRequest body =
            new OrderRequest(details.getCardNumber(), details.getCardOwner(), details.getChecksum(), session.getId());

        // Request and send
        RequestEntity<OrderRequest> request =
            new RequestEntity<>(body, HttpMethod.POST, URI.create(urlConfirm));
        template.exchange(request, void.class);

        // Set view
        model.addAttribute("title", "Confirmed");

        // TODO : Display confirmation message :) / (or Failure)

        return "category";
    }
}
