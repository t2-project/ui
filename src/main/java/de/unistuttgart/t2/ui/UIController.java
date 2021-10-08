package de.unistuttgart.t2.ui;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import de.unistuttgart.t2.common.OrderRequest;
import de.unistuttgart.t2.common.Product;
import de.unistuttgart.t2.common.UpdateCartRequest;
import de.unistuttgart.t2.ui.domain.ItemToAdd;
import de.unistuttgart.t2.ui.domain.PaymentDetails;

/**
 * Defines the http enpoints of the UI.
 * 
 * @author maumau
 *
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
        model.addAttribute("title", "T2 Store");
        return "index";
    }
    
    @GetMapping("/ui/products")
    public String products(Model model) {
        model.addAttribute("title", "Products");
        model.addAttribute("item", new ItemToAdd());
        
        List<Product> products = template.exchange(urlProductsAll, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<List<Product>>(){}).getBody();
        
        model.addAttribute("productslist", products);
        
        return "category";
    }
    
    @GetMapping("/ui/cart")
    public String cart(Model model, HttpSession session) {
        model.addAttribute("title", "Cart");
        model.addAttribute("item", new ItemToAdd());
        
        LOG.info("SessionID : " + session.getId());
        
        // Request and send
        RequestEntity<Void> request = new RequestEntity<Void>(HttpMethod.GET, URI.create(urlCart+session.getId()));        
        ResponseEntity<List<Product>> response = template.exchange(request, new ParameterizedTypeReference<List<Product>>(){});
        
        //Set View
        List<Product> products = response.getBody();
        model.addAttribute("OrderItems", products);
        
        return "cart";
    }

    @GetMapping("/ui/confirm")
    public String confirm(Model model, HttpSession session) {
        
        model.addAttribute("title", "Confirm");
        model.addAttribute("details", new PaymentDetails ());
        
        return "order";        
    }
    
   
    ////////// ACTIONS /////////////
   
    @PostMapping("/ui/add")
    public String add(@ModelAttribute("item") ItemToAdd item, HttpSession session) {
         
        LOG.info("SessionID : " + session.getId());
        LOG.info("Item to Add : " + item.toString());
        
        // Body
        UpdateCartRequest body = new UpdateCartRequest(Map.of(item.getProductId(), item.getUnits()));
        RequestEntity<UpdateCartRequest> request = new RequestEntity<UpdateCartRequest>(body, HttpMethod.POST, URI.create(urlCart+session.getId()));        
        
        // Request and send
        ResponseEntity<List<Product>> response = template.exchange(request, new ParameterizedTypeReference<List<Product>>(){});
        
        
        LOG.info(response.getBody().toString());
        
        return "product";
    }
       
    @PostMapping("/ui/delete")
    public RedirectView delete(@ModelAttribute("item") ItemToAdd item, RedirectAttributes redirectAttributes, HttpSession session) {
         
        LOG.info("SessionID : " + session.getId());
        LOG.info("Item to Delete : " + item.toString());
        
        // Body
        UpdateCartRequest body = new UpdateCartRequest(Map.of(item.getProductId(), -1 * item.getUnits()));
        template.postForEntity(URI.create(urlCart+session.getId()), body, Void.class);
        
        //TODO redirect : to display deleted products
        
        final RedirectView redirectView = new RedirectView("/ui/cart", true);
        return redirectView;
    }

    
    @PostMapping("/ui/confirm")
    public String confirm(@ModelAttribute("details") PaymentDetails details, Model model, HttpSession session) {
        LOG.info("SessionID : " + session.getId());
        
        //Body
        OrderRequest body = new OrderRequest(details.getCardNumber(), details.getCardOwner(), details.getChecksum(), session.getId());
        
        // Request and send
        RequestEntity<OrderRequest> request = new RequestEntity<OrderRequest>(body, HttpMethod.POST, URI.create(urlConfirm));        
        template.exchange(request, void.class);
       
        //Set view
        model.addAttribute("title", "Confirmed");

        // TODO : Display confirmation message :) / (or Failure) 
        
        return "category";        
    }
}