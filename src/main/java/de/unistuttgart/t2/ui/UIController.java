package de.unistuttgart.t2.ui;

import de.unistuttgart.t2.common.OrderRequest;
import de.unistuttgart.t2.common.Product;
import de.unistuttgart.t2.common.UpdateCartRequest;
import de.unistuttgart.t2.ui.domain.ItemToAdd;
import de.unistuttgart.t2.ui.domain.PaymentDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Defines the http endpoints of the UI.
 *
 * @author maumau
 */
@Controller
@RequestMapping("/ui")
public class UIController {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private RestTemplate template;

    private final String urlProductsAll;
    private final String urlCart;
    private final String urlConfirm;

    public UIController(@Value("${t2.uibackend.url}") String urlUiBackend) {
        urlProductsAll = urlUiBackend + "products";
        urlCart = urlUiBackend + "cart";
        urlConfirm = urlUiBackend + "confirm";
    }

    ////// PAGES TO REALLY LOOK AT ///////////

    @GetMapping({"", "/"})
    public String index(Model model) {
        model.addAttribute("title", "T2-Project");
        return "index";
    }

    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("title", "Products");
        model.addAttribute("item", new ItemToAdd());

        List<Product> products = template.exchange(urlProductsAll, HttpMethod.GET, HttpEntity.EMPTY,
            new ParameterizedTypeReference<List<Product>>() {}).getBody();

        model.addAttribute("productslist", products);

        return "category";
    }

    @GetMapping("/cart")
    public String cart(Model model, HttpSession session) {
        model.addAttribute("title", "Cart");
        model.addAttribute("item", new ItemToAdd());

        // Request and send
        RequestEntity<Void> request = new RequestEntity<>(HttpMethod.GET, URI.create(urlCart + "/" + session.getId()));
        ResponseEntity<List<Product>> response = template.exchange(request, new ParameterizedTypeReference<>() {});

        // Set View
        List<Product> products = response.getBody();
        model.addAttribute("OrderItems", products);

        return "cart";
    }

    @GetMapping("/confirm")
    public String confirm(Model model, HttpSession session) {

        model.addAttribute("title", "Confirm");
        model.addAttribute("details", new PaymentDetails());

        return "order";
    }

    ////////// ACTIONS /////////////

    @PostMapping("/add")
    public String add(@ModelAttribute("item") ItemToAdd item, Model model, HttpSession session) {

        LOG.debug("Add item to card: {} | SessionID: {}", item.toString(), session.getId());

        // Body
        UpdateCartRequest body = new UpdateCartRequest(Map.of(item.getProductId(), item.getUnits()));
        RequestEntity<UpdateCartRequest> request =
            new RequestEntity<>(body, HttpMethod.POST, URI.create(urlCart + "/" + session.getId()));

        // Request and send
        try {
            ResponseEntity<List<Product>> response = template.exchange(request, new ParameterizedTypeReference<>() {});
            LOG.info(response.getBody().toString());
        } catch(RestClientException e) {
            LOG.error(e.getMessage());
            model.addAttribute("messagetitle", "Adding item to cart failed!");
            model.addAttribute("messageparagraph", e.getMessage());
            return "error_page";
        }

        return "product";
    }

    @PostMapping("/delete")
    public RedirectView delete(@ModelAttribute("item") ItemToAdd item, RedirectAttributes redirectAttributes,
        HttpSession session) {

        LOG.debug("Delete item from card: {} | SessionID: {}", item.toString(), session.getId());

        // Body
        UpdateCartRequest body = new UpdateCartRequest(Map.of(item.getProductId(), -1 * item.getUnits()));
        template.postForEntity(URI.create(urlCart + "/" + session.getId()), body, Void.class);

        // TODO redirect : to display deleted products

        return new RedirectView("/ui/cart", true);
    }

    @PostMapping("/confirm")
    public String confirm(@ModelAttribute("details") PaymentDetails details, Model model, HttpSession session) {

        LOG.debug("Confirm order | SessionID: {}", session.getId());

        // Body
        OrderRequest body =
            new OrderRequest(details.getCardNumber(), details.getCardOwner(), details.getChecksum(), session.getId());

        // Request and send
        RequestEntity<OrderRequest> request =
            new RequestEntity<>(body, HttpMethod.POST, URI.create(urlConfirm));
        try {
            template.exchange(request, void.class);
            model.addAttribute("title", "Confirmed");
        } catch(RestClientException e) {
            LOG.error(e.getMessage());
            model.addAttribute("messagetitle", "Confirm order failed!");
            model.addAttribute("messageparagraph", e.getMessage());
            return "error_page";
        }

        // Set view
        model.addAttribute("message", "Order was executed successfully!");

        return "category";
    }

    ////////// UNDEFINED /////////////

    @RequestMapping("/**")
    public String error(Model model, HttpServletRequest request) {

        LOG.warn("Unknown UI path requested: " + request.getRequestURI());

        model.addAttribute("title", "Error");

        return "error_page";
    }
}
