import React from "react";
import {render, screen} from "@testing-library/react";
import RoundRecap from "./RoundRecap";
const closed={id:10,state:"FINISHED",answer:{id:91,image:{id:0}},clue:"A new world"};
test("only a closed round can expose its storyteller card, including image zero",()=>{
 const {rerender}=render(<RoundRecap game={{turns:[{...closed,state:"SHOWING_CARDS"}]}} />);
 expect(screen.queryByAltText("Previous storyteller’s card")).not.toBeInTheDocument();
 rerender(<RoundRecap game={{turns:[closed,{id:11,state:"WAITING_STORYTELLER_CARD"}]}} />);
 expect(screen.getByAltText("Previous storyteller’s card")).toHaveAttribute("src","/Cards/0.png");
 expect(screen.getByText("A new world")).toBeInTheDocument();
});
test.each([{cancelled:true},{endDate:"finished"}])("does not replace final/cancelled results: %j",flags=>{
 render(<RoundRecap game={{turns:[closed],...flags}} />);
 expect(screen.queryByText("Previous round · reveal")).not.toBeInTheDocument();
});
