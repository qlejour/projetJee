package projetJee.web.controller;


import ProjetJee.core.entity.Cat;
import ProjetJee.core.entity.Post;
import ProjetJee.core.entity.User;
import ProjetJee.core.service.CatService;
import ProjetJee.core.service.PostService;
import ProjetJee.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

@Controller
public class KittenController {

    private static final Logger LOGGER = LoggerFactory.getLogger(KittenController.class);

    private static final String IMAGE_DIRECTORY_PATH = "C:/kittiesImages";

    @Inject
    private UserService userService;
    @Inject
    private PostService postService;
    @Inject
    private CatService catService;

    @RequestMapping(value = "/user/picture/{postId}", method = RequestMethod.GET)
    public void displayPicture(@PathVariable("postId") long postId, HttpServletResponse response){
        Path path = Paths.get(postService.findById(postId).getPath());
        response.setContentType("image/jpeg");
        try {
            Files.copy(path, response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping(value = "/user/home", method = RequestMethod.GET)
    public String getAllKitties(ModelMap model, HttpServletRequest rq) {
        String email = rq.getSession().getAttribute("userConnecte").toString();
        User userConnected=userService.findByEmail(email);
        List<Post> allPost = postService.findAll();

        rq.getSession().removeAttribute("erreurConnexion");
        rq.getSession().removeAttribute("erreurCreation");
        model.addAttribute("user", userConnected);
        model.addAttribute("posts", allPost);

        return "home";
    }

    @RequestMapping(value = "/user/favourite", method = RequestMethod.GET)
    public String getAllFavouriteKitties(ModelMap model, HttpServletRequest rq) {
        String email = rq.getSession().getAttribute("userConnecte").toString();
        List<Post> allPost = postService.findByUsersFans(userService.findByEmail(email));
        model.addAttribute("user", userService.findByEmail(email));
        model.addAttribute("posts", allPost);

        return "home";
    }


    @RequestMapping(value = "/deconnexion", method = RequestMethod.GET)
    public String deconnexion(HttpServletRequest rq) {
        rq.getSession().removeAttribute("userConnecte");
        return "redirect:/connexion";
    }

    @RequestMapping(value = "/connexion", method = RequestMethod.GET)
    public String showConnexion(ModelMap model, HttpServletRequest rq) {
        model.addAttribute("user", new User());
        model.addAttribute("erreur",rq.getSession().getAttribute("erreurConnexion"));
        return "connexion";
    }

    @RequestMapping(value = "/connexion", method = RequestMethod.POST)
    public String connect(@ModelAttribute("user") User user, HttpServletRequest rq) {
        if (userService.checkPasseword(user.getEmail(), user.getPassword())) {
            LOGGER.info("connexion success");
            User userConnected = userService.findByEmail(user.getEmail());
            rq.getSession().setAttribute("userConnecte", userConnected.getEmail());
            return "redirect:/user/home";
        } else {
            LOGGER.info("connexion failed");
            rq.getSession().setAttribute("erreurConnexion","erreur");
            return "redirect:connexion";
        }
    }

    @RequestMapping(value = "/")
    public String redirectConnexion () {
        return "redirect:connexion";
    }

    @RequestMapping(value = "/user/{userId}/cat/{catId}/ajouterPost", method = RequestMethod.GET)
    public String showAddPost(ModelMap model, @PathVariable("userId") long userId, @PathVariable("catId") long catId,HttpServletRequest rq) {
        Post post = new Post();
        User userconnected=userService.findById(userId);

        if(rq.getSession().getAttribute("userConnecte").toString().equals(userconnected.getEmail())&&catService.isUserCat(userconnected,catId))
        {
            model.addAttribute("user",userconnected);
            model.addAttribute("newPost", post);
            return "ajoutPost";
        }
        else
        {
            LOGGER.warn("essayerez vous de nous duper ?!");
            return "redirect:/user/home";
        }
    }

    @RequestMapping(value = "/user/{userId}/cat/{catId}/ajouterPost", method = RequestMethod.POST)
    public String addPost(@ModelAttribute("newPost") Post post, @PathVariable("userId") long userId, @PathVariable("catId") long catId, @RequestParam("image") MultipartFile picture) {
        Path picturePath = null;
        if (!picture.isEmpty()){
            picturePath = Paths.get(IMAGE_DIRECTORY_PATH, picture.getOriginalFilename());
            try{
                Files.copy(picture.getInputStream(),picturePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        post.setPath(picturePath.toString());
        post.setUser(userService.findById(userId));
        post.setCat(catService.findById(catId));
        post.setDate(new Date());
        postService.savePost(post);
        return "redirect:/user/home";
    }

    @RequestMapping(value = "/creationCompte", method = RequestMethod.GET)
    public String showCreation(ModelMap model,HttpServletRequest rq) {
        model.addAttribute("user", new User());
        model.addAttribute("erreur",rq.getSession().getAttribute("erreurCreation"));
        return "creerUnCompte";
    }

    @RequestMapping(value = "/creationCompte", method = RequestMethod.POST)
    public String addUser(@ModelAttribute("user") User user, HttpServletRequest rq) {
        if(userService.findByEmail(user.getEmail())!=null)
        {
            rq.getSession().setAttribute("erreurCreation","erreur");
            return "redirect:/creationCompte";
        }

      else
        {
            try
            {
                userService.saveUser(user);
                rq.getSession().setAttribute("userConnecte", user.getEmail());
                return "redirect:/user/home";
            }
            catch (javax.persistence.NonUniqueResultException e)
            {
                rq.getSession().setAttribute("erreurCreation","erreur");
                return "redirect:/creationCompte";
            }
        }

    }

    @RequestMapping(value = "/user/{id}/choisirUnChat", method = RequestMethod.GET)
    public String chooseCat(ModelMap model, @PathVariable("id") long id, HttpServletRequest rq) {
        if(rq.getSession().getAttribute("userConnecte").toString().equals(userService.findById(id).getEmail()))
        {
            final List<Cat> allUserCat = catService.findByUser(userService.findById(id));
            Cat cat = new Cat();
            model.addAttribute("newCat", cat);
            model.addAttribute("userCats", allUserCat);
            model.addAttribute("user", userService.findById(id));
            return "chooseCat";

        }
        else
        {
            LOGGER.warn("essayerez vous de nous duper ?!");
            return "redirect:/user/home";
        }

    }

    @RequestMapping(value = "/user/{id}/choisirUnChat", method = RequestMethod.POST)
    public String addCat(@ModelAttribute("newCat") Cat cat, @PathVariable("id") long id) {
        cat.setUser(userService.findById(id));
        if(!cat.getName().equals(""))
        {
            catService.saveCat(cat);
        }

        return "redirect:/user/"+id+"/choisirUnChat";
    }

    @RequestMapping(value = "/addToFavorite&idPost={idPost}&idUser={idUser}", method = RequestMethod.GET)
    public String addToFavorite(@PathVariable("idPost") long idPost, @PathVariable("idUser") long idUser) {
            try
            {
                userService.addFavorite(postService.findById(idPost), userService.findById(idUser));
                LOGGER.info("new favorite post add");
                return "redirect:/user/home";
            }
            catch (java.lang.IllegalStateException e)
            {
                userService.deleteFavorite(postService.findById(idPost), userService.findById(idUser));
                LOGGER.info("new favorite post delete");
                return "redirect:/user/home";
            }

    }


}
